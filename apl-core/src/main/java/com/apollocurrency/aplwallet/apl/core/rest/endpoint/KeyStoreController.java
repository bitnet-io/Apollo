package com.apollocurrency.aplwallet.apl.core.rest.endpoint;

import com.apollocurrency.aplwallet.api.dto.account.WalletKeysInfoDTO;
import com.apollocurrency.aplwallet.api.dto.auth.Status2FA;
import com.apollocurrency.aplwallet.api.dto.auth.TwoFactorAuthParameters;
import com.apollocurrency.aplwallet.apl.core.http.HttpParameterParserUtil;
import com.apollocurrency.aplwallet.apl.core.http.JSONResponses;
import com.apollocurrency.aplwallet.apl.core.http.ParameterException;
import com.apollocurrency.aplwallet.apl.core.model.ExportKeyStore;
import com.apollocurrency.aplwallet.apl.core.service.appdata.SecureStorageService;
import com.apollocurrency.aplwallet.apl.util.JSON;
import com.apollocurrency.aplwallet.apl.util.StringUtils;
import com.apollocurrency.aplwallet.apl.util.ThreadUtils;
import com.apollocurrency.aplwallet.apl.util.builder.ResponseBuilder;
import com.apollocurrency.aplwallet.apl.util.exception.ApiErrors;
import com.apollocurrency.aplwallet.vault.KeyStoreService;
import com.apollocurrency.aplwallet.vault.model.EthWalletKey;
import com.apollocurrency.aplwallet.vault.model.KMSResponseStatus;
import com.apollocurrency.aplwallet.vault.model.WalletKeysInfo;
import com.apollocurrency.aplwallet.vault.service.KMSv1;
import com.apollocurrency.aplwallet.vault.service.auth.Account2FAService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import org.apache.commons.fileupload.FileItemIterator;
import org.apache.commons.fileupload.FileItemStream;
import org.apache.commons.fileupload.servlet.ServletFileUpload;
import org.apache.commons.io.IOUtils;
import org.jboss.resteasy.annotations.jaxrs.FormParam;
import org.web3j.crypto.CipherException;
import org.web3j.crypto.Wallet;
import org.web3j.crypto.WalletFile;

import javax.annotation.security.PermitAll;
import javax.inject.Inject;
import javax.inject.Singleton;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.Consumes;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;

import static com.apollocurrency.aplwallet.apl.core.http.BlockEventSource.LOG;

@Path("/keyStore")
@Singleton
public class KeyStoreController {

    private KeyStoreService keyStoreService;
    private KMSv1 kmSv1;
    private SecureStorageService secureStorageService;
    private Account2FAService account2FAService;
    private Integer maxKeyStoreSize;

    @Inject
    public KeyStoreController(KeyStoreService keyStoreService, KMSv1 kmSv1, SecureStorageService secureStorageService,
                              Account2FAService account2FAService, Integer maxKeyStoreSize) {
        this.keyStoreService = keyStoreService;
        this.kmSv1 = kmSv1;
        this.secureStorageService = secureStorageService;
        this.account2FAService = account2FAService;
        this.maxKeyStoreSize = maxKeyStoreSize;
    }

    @POST
    @Path("/accountInfo")
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = "Get user eth key.",
        tags = {"keyStore"},
        responses = {
            @ApiResponse(responseCode = "200", description = "Successful execution",
                content = @Content(mediaType = "application/json",
                    schema = @Schema(implementation = Response.class)))
        }
    )
    @PermitAll
    public Response getAccountInfo(@FormParam("account") String account,
                                   @FormParam("passphrase") String passphraseReq) throws ParameterException {
        String passphraseStr = HttpParameterParserUtil.getPassphrase(passphraseReq, true);
        long accountId = HttpParameterParserUtil.getAccountId(account, "account", true);

        if (!kmSv1.isWalletExist(accountId)) {
            return Response.status(Response.Status.OK)
                .entity(JSON.toString(
                    JSONResponses.vaultWalletError(accountId,
                        "get account information", "Key for this account is not exist.")
                    )
                ).build();
        }

        WalletKeysInfoDTO keyStoreInfo = kmSv1.getWalletInfo(accountId, passphraseStr);

        if(keyStoreInfo == null){
            return Response.status(Response.Status.OK)
                .entity(JSON.toString(
                    JSONResponses.vaultWalletError(0, "import",
                        "KeyStore or passPhrase is not valid.")
                    )
                ).build();
        }

         secureStorageService.addUserPassPhrase(accountId, passphraseStr);
         return Response.ok(keyStoreInfo).build();
    }


    @POST
    @Path("/upload")
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = "Import keystore container. (file)",
        tags = {"keyStore"},
        responses = {
            @ApiResponse(responseCode = "200", description = "Successful execution",
                content = @Content(mediaType = "application/json",
                    schema = @Schema(implementation = Response.class)))
        }
    )
    @PermitAll
    public Response importKeyStore(@Context HttpServletRequest request) {
        // Check that we have a file upload request
        if (!ServletFileUpload.isMultipartContent(request)) {
            return Response.status(Response.Status.BAD_REQUEST).build();
        }

        byte[] keyStore = null;
        String passPhrase = null;

        try {
            ServletFileUpload upload = new ServletFileUpload();
            // Set overall request size constraint
            upload.setSizeMax(Long.valueOf(maxKeyStoreSize));
            FileItemIterator fileIterator = upload.getItemIterator(request);

            while (fileIterator.hasNext()) {
                FileItemStream item = fileIterator.next();
                if ("keyStore".equals(item.getFieldName())) {
                    keyStore = IOUtils.toByteArray(item.openStream());
                } else if ("passPhrase".equals(item.getFieldName())) {
                    passPhrase = IOUtils.toString(item.openStream());
                } else {
                    return Response.status(Response.Status.OK)
                        .entity(JSON.toString(
                            JSONResponses.vaultWalletError(0, "import",
                                "Failed to upload file. Unknown parameter: " + item.getFieldName())
                            )
                        ).build();
                }
            }

            if (passPhrase == null || keyStore == null || keyStore.length == 0) {
                return Response.status(Response.Status.OK)
                    .entity(JSON.toString(
                        JSONResponses.vaultWalletError(0, "import",
                            "Parameter 'passPhrase' or 'keyStore' is null")
                        )
                    ).build();
            }

            KMSResponseStatus status = kmSv1.storeWallet(keyStore, passPhrase);

            if (status.isOK()) {
                return Response.status(200).build();
            } else {
                return Response.status(Response.Status.OK)
                    .entity(JSON.toString(
                        JSONResponses.vaultWalletError(0, "import", status.message)
                        )
                    ).build();
            }
        } catch (Exception ex) {
            LOG.error(ex.getMessage(), ex);
            return Response.status(Response.Status.BAD_REQUEST).entity("Failed to upload file.").build();
        }

    }


    @POST
    @Path("/download")
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = "Export keystore container. (file)",
        tags = {"keyStore"},
        responses = {
            @ApiResponse(responseCode = "200", description = "Successful execution",
                content = @Content(mediaType = "multipart/form-data",
                    schema = @Schema(implementation = Response.class)))
        }
    )
    @PermitAll
    public Response downloadKeyStore(@FormParam("account") String account,
                                     @FormParam("passPhrase") String passphraseReq, @Context HttpServletRequest request) throws ParameterException, IOException {
        try {
            String passphraseStr = HttpParameterParserUtil.getPassphrase(passphraseReq, true);
            long accountId = HttpParameterParserUtil.getAccountId(account, "account", true);


            if (!keyStoreService.isKeyStoreForAccountExist(accountId)) {
                return Response.status(Response.Status.OK)
                    .entity(JSON.toString(JSONResponses.vaultWalletError(accountId,
                        "get account information", "Key for this account is not exist."))
                    ).build();
            }

            if (account2FAService.isEnabled2FA(accountId)) {
                int code2FA = HttpParameterParserUtil.getInt(request, "code2FA", 0, Integer.MAX_VALUE, false);
                TwoFactorAuthParameters twoFactorAuthParameters = new TwoFactorAuthParameters(accountId, passphraseStr, null);
                twoFactorAuthParameters.setCode2FA(code2FA);

                Status2FA status2FA = account2FAService.verify2FA(twoFactorAuthParameters);
                if (!status2FA.OK.equals(status2FA)) {
                    return Response.status(Response.Status.OK).entity(JSON.toString(JSONResponses.error2FA(status2FA, accountId))).build();
                }
            }

            File keyStore = keyStoreService.getSecretStoreFile(accountId, passphraseStr);

            if (keyStore == null) {
                throw new ParameterException(JSONResponses.incorrect("account id or passphrase"));
            }

            Response.ResponseBuilder response = Response.ok(new ExportKeyStore(Files.readAllBytes(keyStore.toPath()), keyStore.getName()).toJSON());
            return response.build();
        } catch (ParameterException ex) {
            return Response.status(Response.Status.OK).entity(JSON.toString(ex.getErrorResponse())).build();
        }
    }

    @POST
    @Path("/eth")
//    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(tags = {"keyStore"}, summary = "Export eth keystore",
        description = "Generate eth keystore for specified account in json format fully compatible with original geth keystore. Required 2fa code for accounts with enabled 2fa.",
        responses = @ApiResponse(description = "Eth wallet keystore for account in json format", responseCode = "200",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = WalletFile.class))))
    public Response downloadEthKeyStore(@Parameter(description = "Apl account id or rs", required = true) @FormParam("account") String account,
                                        @Parameter(description = "Eth account address", required = true) @FormParam("ethAddress") String ethAccountAddress,
                                        @Parameter(description = "Passphrase for apl vault account", required = true) @FormParam("passphrase") String passphrase,
                                        @Parameter(description = "New password to encrypt eth key, if omitted apl passphrase will be used instead (not recommended)") @FormParam("ethKeystorePassword") String ethKeystorePassword,
                                        @Parameter(description = "2fa code for account if enabled") @FormParam("code2FA") @DefaultValue("0") int code) throws ParameterException {
        String aplVaultPassphrase = HttpParameterParserUtil.getPassphrase(passphrase, true);
        long accountId = HttpParameterParserUtil.getAccountId(account, "account", true);

        TwoFactorAuthParameters twoFactorAuthParameters = new TwoFactorAuthParameters(accountId, aplVaultPassphrase, null);
        twoFactorAuthParameters.setCode2FA(code);
        account2FAService.verify2FA(twoFactorAuthParameters);

        if (!keyStoreService.isKeyStoreForAccountExist(accountId)) {
            return Response.status(Response.Status.OK)
                .entity(JSON.toString(JSONResponses.vaultWalletError(accountId,
                    "get account information", "Key for this account is not exist."))
                ).build();
        }

        WalletKeysInfo keysInfo = keyStoreService.getWalletKeysInfo(aplVaultPassphrase, accountId);

        if (keysInfo == null) {
            throw new ParameterException(JSONResponses.incorrect("account id or passphrase"));
        }

        String passwordToEncryptEthKeystore = StringUtils.isBlank(ethKeystorePassword) ? aplVaultPassphrase : ethKeystorePassword;
        EthWalletKey ethKeyToExport = keysInfo.getEthWalletForAddress(ethAccountAddress);
        if (ethKeyToExport == null) {
            throw new ParameterException(JSONResponses.incorrect("ethAddress"));
        }
        try {
            WalletFile walletFile = Wallet.createStandard(passwordToEncryptEthKeystore, ethKeyToExport.getCredentials().getEcKeyPair());
            return Response.ok(walletFile).build();
        } catch (CipherException e) {
            return ResponseBuilder.apiError(ApiErrors.WEB3J_CRYPTO_ERROR, ThreadUtils.getStackTraceSilently(e), e.getMessage()).build();
        }
    }
}

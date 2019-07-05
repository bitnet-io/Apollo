/*
 *  Copyright © 2018-2019 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.core.rest.endpoint;

import com.apollocurrency.aplwallet.api.dto.ShardDTO;
import com.apollocurrency.aplwallet.apl.core.rest.converter.ShardToDtoConverter;
import com.apollocurrency.aplwallet.apl.core.shard.ShardService;
import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.tags.Tag;

import java.util.List;
import java.util.stream.Collectors;
import javax.inject.Inject;
import javax.inject.Singleton;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

@Path("/shards")
@OpenAPIDefinition(tags = {@Tag(name = "/shards")}, info = @Info(description = "Provide data about shards"))
@Singleton
public class ShardController {

    private ShardService shardService;
//    @Inject @Setter
    private ShardToDtoConverter shardConverter = new ShardToDtoConverter();

    @Inject
    public ShardController(ShardService shardService) {
        this.shardService = shardService;
    }

    public ShardController() {
    }

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(tags = {"shards"}, summary = "Retrieve all completed only shards", description = "Get all 'completed' shard entries from database")
    public Response getAllShards() {
        List<ShardDTO> allCompletedShards = shardService.getAllCompletedShards().stream()
                .map(shard -> shardConverter.convert(shard)).collect(Collectors.toList());
        return Response.ok(allCompletedShards).build();
    }
    @POST
    @Path("/{id}")
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(tags = {"shards"}, summary = "Reset node to backup db before sharding specified by id", description = "Find backup before shard with specified id and when it exists - we will drop our blockchain and load backup entirely")
    public Response resetToBackup(@PathParam("id") long shardId) {
        return Response.ok(shardService.reset(shardId)).build();
    }



}

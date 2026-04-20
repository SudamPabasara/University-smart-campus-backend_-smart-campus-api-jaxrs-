package com.smartcampus.resource;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.Map;

import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import com.smartcampus.data.DataStore;
import com.smartcampus.exception.RoomNotEmptyException;
import com.smartcampus.model.Room;

@Path("/rooms")
@Produces(MediaType.APPLICATION_JSON)
public class RoomResource {

    private final DataStore dataStore = DataStore.getInstance();

    @GET
    public Response getAllRooms() {
        return Response.ok(new ArrayList<>(dataStore.getRooms().values())).build();
    }

    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    public Response createRoom(Room room) {
        if (room.getId() == null || room.getId().trim().isEmpty()) {
            Map<String, Object> error = new LinkedHashMap<>();
            error.put("error", "Bad Request");
            error.put("status", 400);
            error.put("message", "Room ID is required.");
            return Response.status(Response.Status.BAD_REQUEST).entity(error).build();
        }

        if (dataStore.getRooms().containsKey(room.getId())) {
            Map<String, Object> error = new LinkedHashMap<>();
            error.put("error", "Conflict");
            error.put("status", 409);
            error.put("message", "A room with ID '" + room.getId() + "' already exists.");
            return Response.status(Response.Status.CONFLICT).entity(error).build();
        }

        dataStore.getRooms().put(room.getId(), room);
        return Response.status(Response.Status.CREATED).entity(room).build();
    }

    @GET
    @Path("/{roomId}")
    public Response getRoomById(@PathParam("roomId") String roomId) {
        Room room = dataStore.getRooms().get(roomId);
        if (room == null) {
            Map<String, Object> error = new LinkedHashMap<>();
            error.put("error", "Not Found");
            error.put("status", 404);
            error.put("message", "Room with ID '" + roomId + "' was not found.");
            return Response.status(Response.Status.NOT_FOUND).entity(error).build();
        }
        return Response.ok(room).build();
    }

    @DELETE
    @Path("/{roomId}")
    public Response deleteRoom(@PathParam("roomId") String roomId) {
        Room room = dataStore.getRooms().get(roomId);
        if (room == null) {
            Map<String, Object> error = new LinkedHashMap<>();
            error.put("error", "Not Found");
            error.put("status", 404);
            error.put("message", "Room with ID '" + roomId + "' was not found.");
            return Response.status(Response.Status.NOT_FOUND).entity(error).build();
        }

        if (room.getSensorIds() != null && !room.getSensorIds().isEmpty()) {
            throw new RoomNotEmptyException(
                    "Cannot delete room '" + roomId + "' because it still has " +
                    room.getSensorIds().size() + " sensor(s) assigned to it."
            );
        }

        dataStore.getRooms().remove(roomId);
        return Response.noContent().build();
    }
}

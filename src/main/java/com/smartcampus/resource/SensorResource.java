package com.smartcampus.resource;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import com.smartcampus.data.DataStore;
import com.smartcampus.exception.LinkedResourceNotFoundException;
import com.smartcampus.model.Room;
import com.smartcampus.model.Sensor;

@Path("/sensors")
@Produces(MediaType.APPLICATION_JSON)
public class SensorResource {

    private final DataStore dataStore = DataStore.getInstance();

    @GET
    public Response getAllSensors(@QueryParam("type") String type) {
        List<Sensor> sensors = new ArrayList<>(dataStore.getSensors().values());

        if (type != null && !type.trim().isEmpty()) {
            sensors = sensors.stream()
                    .filter(s -> s.getType().equalsIgnoreCase(type.trim()))
                    .collect(Collectors.toList());
        }

        return Response.ok(sensors).build();
    }

    @GET
    @Path("/{sensorId}")
    public Response getSensorById(@PathParam("sensorId") String sensorId) {
        Sensor sensor = dataStore.getSensors().get(sensorId);
        if (sensor == null) {
            Map<String, Object> error = new LinkedHashMap<>();
            error.put("error", "Not Found");
            error.put("status", 404);
            error.put("message", "Sensor with ID '" + sensorId + "' was not found.");
            return Response.status(Response.Status.NOT_FOUND).entity(error).build();
        }
        return Response.ok(sensor).build();
    }

    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    public Response createSensor(Sensor sensor) {
        if (sensor.getId() == null || sensor.getId().trim().isEmpty()) {
            Map<String, Object> error = new LinkedHashMap<>();
            error.put("error", "Bad Request");
            error.put("status", 400);
            error.put("message", "Sensor ID is required.");
            return Response.status(Response.Status.BAD_REQUEST).entity(error).build();
        }

        if (dataStore.getSensors().containsKey(sensor.getId())) {
            Map<String, Object> error = new LinkedHashMap<>();
            error.put("error", "Conflict");
            error.put("status", 409);
            error.put("message", "A sensor with ID '" + sensor.getId() + "' already exists.");
            return Response.status(Response.Status.CONFLICT).entity(error).build();
        }

        // Validate that the roomId exists
        if (sensor.getRoomId() == null || sensor.getRoomId().trim().isEmpty()) {
            Map<String, Object> error = new LinkedHashMap<>();
            error.put("error", "Bad Request");
            error.put("status", 400);
            error.put("message", "Room ID is required for sensor registration.");
            return Response.status(Response.Status.BAD_REQUEST).entity(error).build();
        }

        Room room = dataStore.getRooms().get(sensor.getRoomId());
        if (room == null) {
            throw new LinkedResourceNotFoundException(
                    "The room with ID '" + sensor.getRoomId() + "' does not exist. " +
                    "Cannot register sensor to a non-existent room."
            );
        }

        // Link sensor to the room
        dataStore.getSensors().put(sensor.getId(), sensor);
        room.getSensorIds().add(sensor.getId());

        return Response.status(Response.Status.CREATED).entity(sensor).build();
    }

    @DELETE
    @Path("/{sensorId}")
    public Response deleteSensor(@PathParam("sensorId") String sensorId) {
        Sensor sensor = dataStore.getSensors().get(sensorId);
        if (sensor == null) {
            Map<String, Object> error = new LinkedHashMap<>();
            error.put("error", "Not Found");
            error.put("status", 404);
            error.put("message", "Sensor with ID '" + sensorId + "' was not found.");
            return Response.status(Response.Status.NOT_FOUND).entity(error).build();
        }

        // Remove sensor from its room's sensorIds list
        Room room = dataStore.getRooms().get(sensor.getRoomId());
        if (room != null) {
            room.getSensorIds().remove(sensorId);
        }

        dataStore.getSensors().remove(sensorId);
        dataStore.getSensorReadings().remove(sensorId);

        return Response.noContent().build();
    }

    @PUT
    @Path("/{sensorId}")
    @Consumes(MediaType.APPLICATION_JSON)
    public Response updateSensor(@PathParam("sensorId") String sensorId, Sensor updatedSensor) {
        Sensor existing = dataStore.getSensors().get(sensorId);
        if (existing == null) {
            Map<String, Object> error = new LinkedHashMap<>();
            error.put("error", "Not Found");
            error.put("status", 404);
            error.put("message", "Sensor with ID '" + sensorId + "' was not found.");
            return Response.status(Response.Status.NOT_FOUND).entity(error).build();
        }

        if (updatedSensor.getType() != null) {
            existing.setType(updatedSensor.getType());
        }
        if (updatedSensor.getStatus() != null) {
            existing.setStatus(updatedSensor.getStatus());
        }
        existing.setCurrentValue(updatedSensor.getCurrentValue());

        return Response.ok(existing).build();
    }

    // Sub-resource locator for sensor readings (Part 4)
    @Path("/{sensorId}/readings")
    public SensorReadingResource getSensorReadings(@PathParam("sensorId") String sensorId) {
        Sensor sensor = dataStore.getSensors().get(sensorId);
        if (sensor == null) {
            Map<String, Object> error = new LinkedHashMap<>();
            error.put("error", "Not Found");
            error.put("status", 404);
            error.put("message", "Sensor with ID '" + sensorId + "' was not found.");
            throw new WebApplicationException(
                    Response.status(Response.Status.NOT_FOUND).entity(error)
                            .type(MediaType.APPLICATION_JSON).build()
            );
        }
        return new SensorReadingResource(sensorId);
    }
}

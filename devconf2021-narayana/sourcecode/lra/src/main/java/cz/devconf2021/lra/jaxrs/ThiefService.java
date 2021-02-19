package cz.devconf2021.lra.jaxrs;

import java.net.URI;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.HeaderParam;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.Entity;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import cz.devconf2021.lra.jpa.AdventurerTask;
import cz.devconf2021.lra.jpa.TaskType;
import cz.devconf2021.lra.utils.JSONParser;
import io.narayana.lra.client.NarayanaLRAClient;
import org.eclipse.microprofile.lra.annotation.Compensate;
import org.eclipse.microprofile.lra.annotation.Complete;
import org.eclipse.microprofile.lra.annotation.ParticipantStatus;
import org.eclipse.microprofile.lra.annotation.ws.rs.LRA;
import org.jboss.logging.Logger;

import cz.devconf2021.lra.jpa.AdventurerTaskRepository;
import cz.devconf2021.lra.jpa.TaskStatus;

/**
 * <p>
 * Demo REST api showing the usage of <a href="https://github.com/eclipse/microprofile-lra">LRA</a>.
 * <p>
 */
@Path("/thief")
public class ThiefService {
    private static final Logger log = Logger.getLogger(ThiefService.class);

    @Inject
    private AdventurerTaskRepository adventurerTaskRepository;

    @Inject
    private NarayanaLRAClient lraClient;

    /**
     * Thief task of stealing. It creates the LRA context
     * and if <code>target.call</code> is defined in JSON body
     * then the next service is called.
     * <p>
     * Expecting JSON data in format:
     * <code>{"date":"2019-01-27", "name": "Name of passenger", "target.call": "http://localhost:8280/warrior"}</code>
     */
    @LRA
    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    public Response stealSomething(@HeaderParam(LRA.LRA_HTTP_CONTEXT_HEADER) URI lraId, String jsonData) {
        // getting json to map
        Map<String,String> jsonMap = JSONParser.parseJson(jsonData);

        // will we be calling a next service?
        String targetCallFromJson = jsonMap.get("target.call");

        log.infof("Thief task JSON '%s' as part of LRA id '%s', trying to call at '%s'",
                jsonData, lraId.toASCIIString(), targetCallFromJson);

        // do database changes
        AdventurerTask adventurerTask = new AdventurerTask()
                .setName(jsonMap.get("name"))
                .setStatus(TaskStatus.IN_PROGRESS)
                .setType(TaskType.THIEF)
                .setLraId(lraClient.getCurrent().toASCIIString());
        adventurerTaskRepository.save(adventurerTask);
        log.infof("Thief task '%s' was created", adventurerTask);

        // calling next service
        if(targetCallFromJson != null && !targetCallFromJson.isEmpty()) {
            Response response;
            boolean shouldBeCanceled = false;
            try {
                response = ClientBuilder.newClient().target(targetCallFromJson)
                        .request(MediaType.TEXT_PLAIN)
                        .post(Entity.text("help with your strength on  " + adventurerTask.getName()));

                String entityBody = response.readEntity(String.class);
                int returnCode = response.getStatus();
                log.infof("Response code from call '%s' was %s, entity: %s", targetCallFromJson, returnCode, entityBody);
            } catch (Exception e) {
                log.errorf(e, "Failed to call '%s': %s", targetCallFromJson, e.getMessage());
                shouldBeCanceled = true;
            }
            if(shouldBeCanceled) lraClient.cancelLRA(lraClient.getCurrent());
        }

        return Response.ok(adventurerTask.getId()).build();
    }

    /**
     * LRA complete
     */
    @PUT
    @Path("/complete")
    @Produces(MediaType.TEXT_PLAIN)
    @Complete
    public Response completeTask(@HeaderParam(LRA.LRA_HTTP_CONTEXT_HEADER) URI lraId) {
        log.infof("Completing '%s'...", lraId);
        updateStatus(lraId, TaskStatus.SUCCESS);
        log.infof("LRA ID '%s' was completed", lraId);
        return Response.ok(ParticipantStatus.Completed.name()).build();
    }

    /**
     * LRA compensate
     */
    @PUT
    @Path("/compensate")
    @Produces(MediaType.TEXT_PLAIN)
    @Compensate
    public Response compensateTask(@HeaderParam(LRA.LRA_HTTP_CONTEXT_HEADER) URI lraId) {
        log.infof("Compensating '%s'...", lraId);
        updateStatus(lraId, TaskStatus.FAILURE);
        log.warnf("LRA ID '%s' was compensated", lraId);
        return Response.ok(ParticipantStatus.Compensated.name()).build();
    }

    /**
     * Listing database with <code>SELECT *</code>.
     */
    @Path("/all")
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public Response listTasks() {
        List<AdventurerTask> allThiefTasks = adventurerTaskRepository.getByType(TaskType.THIEF);
        if(log.isDebugEnabled()) log.debugf("All thief tasks are: %s", allThiefTasks);
        return Response.ok().entity(allThiefTasks).build();
    }

    private void updateStatus(URI lraId, TaskStatus newStatus) {
        List<AdventurerTask> byLraAdventurerTasks = adventurerTaskRepository.getByLraId(lraId.toASCIIString());
        for(AdventurerTask adventurerTask : byLraAdventurerTasks) {
            adventurerTask.setStatus(newStatus);
            adventurerTaskRepository.update(adventurerTask);
        }
    }
}

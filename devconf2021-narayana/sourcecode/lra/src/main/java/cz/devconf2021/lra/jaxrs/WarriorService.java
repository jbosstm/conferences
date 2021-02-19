package cz.devconf2021.lra.jaxrs;

import cz.devconf2021.lra.jpa.AdventurerTask;
import cz.devconf2021.lra.jpa.AdventurerTaskRepository;
import cz.devconf2021.lra.jpa.TaskStatus;
import cz.devconf2021.lra.jpa.TaskType;
import io.narayana.lra.client.NarayanaLRAClient;
import org.eclipse.microprofile.lra.annotation.Compensate;
import org.eclipse.microprofile.lra.annotation.Complete;
import org.eclipse.microprofile.lra.annotation.ParticipantStatus;
import org.eclipse.microprofile.lra.annotation.ws.rs.LRA;
import org.jboss.logging.Logger;

import javax.inject.Inject;
import javax.ws.rs.GET;
import javax.ws.rs.HeaderParam;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.net.URI;
import java.util.List;

/**
 * <p>
 * Demo REST api showing the usage of <a href="https://github.com/eclipse/microprofile-lra">LRA</a>.
 * <p>
 */
@Path("/warrior")
public class WarriorService {
    private static final Logger log = Logger.getLogger(WarriorService.class);

    @Inject
    private AdventurerTaskRepository adventurerTaskRepository;

    @Inject
    private NarayanaLRAClient lraClient;

    /**
     * Warrior task of fighting.
     */
    @LRA(end = false)
    @POST
    public Response fightInChain(@HeaderParam(LRA.LRA_HTTP_CONTEXT_HEADER) String lraId, String message) {
        if(lraId == null || lraId.isEmpty()) {
            throw new WebApplicationException("LRA ID header [" + LRA.LRA_HTTP_CONTEXT_HEADER + "] is empty", Response.Status.PRECONDITION_FAILED);
        }

        // do database changes
        AdventurerTask adventurerTask = new AdventurerTask()
                .setName(message)
                .setStatus(TaskStatus.IN_PROGRESS)
                .setType(TaskType.WARRIOR)
                .setLraId(lraClient.getCurrent().toASCIIString());
        adventurerTaskRepository.save(adventurerTask);
        log.infof("Warrior task '%s' was created", adventurerTask);
        return Response.ok().build();
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
        log.info("Dooooing"); // TODO: delete me
        List<AdventurerTask> allWarriorTasks = adventurerTaskRepository.getByType(TaskType.WARRIOR);
        if(log.isDebugEnabled()) log.debugf("All warrior tasks are: %s", allWarriorTasks);
        return Response.ok().entity(allWarriorTasks).build();
    }

    private void updateStatus(URI lraId, TaskStatus newStatus) {
        List<AdventurerTask> byLraAdventurerTasks = adventurerTaskRepository.getByLraId(lraId.toASCIIString());
        for(AdventurerTask adventurerTask : byLraAdventurerTasks) {
            adventurerTask.setStatus(newStatus);
            adventurerTaskRepository.update(adventurerTask);
        }
    }
}

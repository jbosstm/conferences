package cz.devconf2021.jta;

import java.util.List;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.transaction.RollbackException;
import javax.transaction.SystemException;
import javax.transaction.TransactionManager;
import javax.transaction.Transactional;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Response;
import javax.ws.rs.ext.ExceptionMapper;
import javax.ws.rs.ext.Provider;

import org.jboss.resteasy.annotations.jaxrs.PathParam;

import org.jboss.logging.Logger;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

import io.quarkus.panache.common.Sort;

@Path("thief")
@ApplicationScoped
@Produces("application/json")
@Consumes("application/json")
public class Resources {
    private static final Logger LOGGER = Logger.getLogger(Resources.class);

    @Inject
    TransactionManager tm;

    @GET
    public List<CloakHiding> get() {
        return CloakHiding.listAll(Sort.by("cloakAction"));
    }

    @POST
    @Path("{cloakAction}")
    @Transactional
    public Response create(@PathParam String cloakAction, CloakHiding cloakHiding) throws SystemException, RollbackException {
        if (cloakAction == null || cloakAction.isEmpty()) {
            throw new WebApplicationException("Expecting some cool cloak action but there is no one.", 422);
        }

        CloakHiding newCloakThing = new CloakHiding(cloakAction);
        newCloakThing.persist();

        // synchronization callback on transaction is ending
        tm.getTransaction().registerSynchronization(new NoticeMe());

        // additional resource to take into transactional unit of workA
        tm.getTransaction().enlistResource(new Lockpicking());

        return Response.ok(newCloakThing).status(201).build();
    }



    @Provider
    public static class ErrorMapper implements ExceptionMapper<Exception> {

        @Inject
        ObjectMapper objectMapper;

        @Override
        public Response toResponse(Exception exception) {
            LOGGER.error("Failed to handle request", exception);

            int code = 500;
            if (exception instanceof WebApplicationException) {
                code = ((WebApplicationException) exception).getResponse().getStatus();
            }

            ObjectNode exceptionJson = objectMapper.createObjectNode();
            exceptionJson.put("exceptionType", exception.getClass().getName());
            exceptionJson.put("code", code);

            if (exception.getMessage() != null) {
                exceptionJson.put("error", exception.getMessage());
            }

            return Response.status(code)
                    .entity(exceptionJson)
                    .build();
        }

    }
}

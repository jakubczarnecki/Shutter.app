package pl.lodz.p.it.ssbd2022.ssbd02.exceptions;

import javax.ws.rs.core.Response;

public class NoAvailabilityFoundException extends BaseApplicationException {

    public NoAvailabilityFoundException(String msg) {
        super(Response.status(Response.Status.NOT_FOUND).entity(msg).build());
    }
}
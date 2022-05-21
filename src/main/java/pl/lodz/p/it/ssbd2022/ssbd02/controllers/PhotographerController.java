package pl.lodz.p.it.ssbd2022.ssbd02.controllers;

import pl.lodz.p.it.ssbd2022.ssbd02.exceptions.NoAuthenticatedAccountFound;
import pl.lodz.p.it.ssbd2022.ssbd02.exceptions.NoPhotographerFound;
import pl.lodz.p.it.ssbd2022.ssbd02.mok.dto.BasePhotographerInfoDto;
import pl.lodz.p.it.ssbd2022.ssbd02.mok.dto.EnhancedPhotographerInfoDto;
import pl.lodz.p.it.ssbd2022.ssbd02.mok.endpoint.PhotographerEndpoint;

import javax.inject.Inject;
import javax.validation.constraints.NotNull;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

@Path("/photographer")
public class PhotographerController {

    @Inject
    PhotographerEndpoint photographerEndpoint;

    /**
     * Punkt końcowy szukający fotografa
     *
     * @param login nazwa użytkownika fotografa
     * @throws NoPhotographerFound         W przypadku gdy fotograf o podanej nazwie użytkownika nie istnieje,
     *                                     gdy konto szukanego fotografa jest nieaktywne, niepotwierdzone lub profil nieaktywny i informacje próbuje uzyskać użytkownik
     *                                     niebędący ani administratorem, ani moderatorem
     * @throws NoAuthenticatedAccountFound W przypadku gdy dane próbuje uzyskać niezalogowana osoba
     * @see BasePhotographerInfoDto
     */
    @GET
    @Path("/{login}/info")
    @Produces(MediaType.APPLICATION_JSON)
    public BasePhotographerInfoDto getPhotographerInfo(@NotNull @PathParam("login") String login) throws NoPhotographerFound, NoAuthenticatedAccountFound {
        return photographerEndpoint.getPhotographerInfo(login);
    }
    /**
     * Punkt końcowy szukający fotografa
     *
     * @param login nazwa użytkownika fotografa
     * @throws NoPhotographerFound         W przypadku gdy fotograf o podanej nazwie użytkownika nie istnieje,
     *                                     gdy konto szukanego fotografa jest nieaktywne, niepotwierdzone lub profil nieaktywny i informacje próbuje uzyskać użytkownik
     *                                     niebędący ani administratorem, ani moderatorem
     * @throws NoAuthenticatedAccountFound W przypadku gdy dane próbuje uzyskać niezalogowana osoba
     * @see BasePhotographerInfoDto
     */
    @GET
    @Path("/{login}/infoplus")
    @Produces(MediaType.APPLICATION_JSON)
    public EnhancedPhotographerInfoDto getEnhancedPhotographerInfo(@NotNull @PathParam("login") String login) throws NoPhotographerFound, NoAuthenticatedAccountFound {
        return photographerEndpoint.getEnhancedPhotographerInfo(login);
    }

    /**
     * Punkt końcowy zwracający informacje o zalogowanym fotografie
     *
     * @throws NoPhotographerFound         W przypadku gdy profil fotografa dla użytkownika nie istnieje
     * @throws NoAuthenticatedAccountFound W przypadku gdy dane próbuje uzyskać niezalogowana osoba
     * @see BasePhotographerInfoDto
     */
    @GET
    @Path("/info")
    @Produces(MediaType.APPLICATION_JSON)
    public EnhancedPhotographerInfoDto getPhotographerInfo() throws NoPhotographerFound, NoAuthenticatedAccountFound {
        return photographerEndpoint.getYourPhotographerInfo();
    }
}

package pl.lodz.p.it.ssbd2022.ssbd02.mok.service;


import pl.lodz.p.it.ssbd2022.ssbd02.entity.AccessLevelAssignment;
import pl.lodz.p.it.ssbd2022.ssbd02.entity.Account;
import pl.lodz.p.it.ssbd2022.ssbd02.entity.PhotographerInfo;
import pl.lodz.p.it.ssbd2022.ssbd02.exceptions.ExceptionFactory;
import pl.lodz.p.it.ssbd2022.ssbd02.exceptions.NoPhotographerFound;
import pl.lodz.p.it.ssbd2022.ssbd02.mok.dto.BasePhotographerInfoDto;
import pl.lodz.p.it.ssbd2022.ssbd02.mok.facade.AuthenticationFacade;
import pl.lodz.p.it.ssbd2022.ssbd02.util.LoggingInterceptor;

import javax.annotation.security.PermitAll;
import javax.annotation.security.RolesAllowed;
import javax.ejb.Stateless;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.inject.Inject;
import javax.interceptor.Interceptors;
import java.util.List;
import java.util.stream.Collectors;

import static pl.lodz.p.it.ssbd2022.ssbd02.security.Roles.*;

@Stateless
@Interceptors(LoggingInterceptor.class)
@TransactionAttribute(TransactionAttributeType.MANDATORY)
public class PhotographerService {

    @Inject
    private AuthenticationFacade accountFacade;



    /**
     * Odnajduje informacje o fotografie na podstawie jego loginu
     *
     * @param login Login fotografa dla którego chemy pozyskać informacje
     * @throws NoPhotographerFound W przypadku kiedy fotograf o danym loginie nie istnieje
     */
    @PermitAll
    public PhotographerInfo findByLogin(String login) throws NoPhotographerFound {
        return accountFacade.findPhotographerByLogin(login);
    }


    /**
     * Szuka fotografa
     *
     * @param requester        Konto użytkownika próbującego uzyskać informacje o danym fotografie
     * @param photographerInfo Informacje o fotografie, które próbuje pozyskać użytkownik
     * @throws NoPhotographerFound W przypadku gdy fotograf o podanej nazwie użytkownika nie istnieje,
     *                             gdy konto szukanego fotografa jest nieaktywne, niepotwierdzone lub
     *                             profil nieaktywny i informacje próbuje uzyskać użytkownik
     *                             niebędący ani administratorem, ani moderatorem
     * @see BasePhotographerInfoDto
     */
    @RolesAllowed(getPhotographerInfo)
    public PhotographerInfo getPhotographerInfo(PhotographerInfo photographerInfo)
            throws NoPhotographerFound {
        if (
                Boolean.TRUE.equals(photographerInfo.getVisible())
                        && Boolean.TRUE.equals(photographerInfo.getAccount().getActive())
                        && Boolean.TRUE.equals(photographerInfo.getAccount().getRegistered())
        ) {
            return photographerInfo;
        } else {
            throw ExceptionFactory.noPhotographerFound();
        }
    }
}

package pl.lodz.p.it.ssbd2022.ssbd02.mow.facade;


import pl.lodz.p.it.ssbd2022.ssbd02.entity.Photo;
import pl.lodz.p.it.ssbd2022.ssbd02.exceptions.BaseApplicationException;
import pl.lodz.p.it.ssbd2022.ssbd02.exceptions.ExceptionFactory;
import pl.lodz.p.it.ssbd2022.ssbd02.util.FacadeTemplate;
import pl.lodz.p.it.ssbd2022.ssbd02.util.LoggingInterceptor;

import javax.annotation.security.PermitAll;
import javax.ejb.Stateless;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.interceptor.Interceptors;
import javax.persistence.*;


@Stateless
@Interceptors({LoggingInterceptor.class})
@TransactionAttribute(TransactionAttributeType.MANDATORY)
public class PhotoFacade extends FacadeTemplate<Photo> {
    @PersistenceContext(unitName = "ssbd02mowPU")
    private EntityManager em;

    public PhotoFacade() {
        super(Photo.class);
    }

    @Override
    @PermitAll
    public EntityManager getEm() {
        return em;
    }


    @Override
    @PermitAll
    public Photo find(Long id) throws BaseApplicationException {
        try {
            return super.find(id);
        } catch (NoResultException e) {
            throw ExceptionFactory.noPhotoFoundException();
        } catch (OptimisticLockException ex) {
            throw ExceptionFactory.OptLockException();
        } catch (PersistenceException ex) {
            throw ExceptionFactory.databaseException();
        } catch (Exception ex) {
            throw ExceptionFactory.unexpectedFailException();
        }
    }
}

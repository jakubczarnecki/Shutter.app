package pl.lodz.p.it.ssbd2022.ssbd02.mow.facade;

import pl.lodz.p.it.ssbd2022.ssbd02.entity.PhotographerReport;
import pl.lodz.p.it.ssbd2022.ssbd02.util.FacadeTemplate;
import pl.lodz.p.it.ssbd2022.ssbd02.util.LoggingInterceptor;
import pl.lodz.p.it.ssbd2022.ssbd02.util.FacadeAccessInterceptor;

import javax.ejb.Stateless;
import javax.interceptor.Interceptors;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;

@Stateless
@Interceptors({LoggingInterceptor.class, FacadeAccessInterceptor.class})
public class PhotographerReportFacade extends FacadeTemplate<PhotographerReport> {
    @PersistenceContext(unitName = "ssbd02mowPU")
    private EntityManager em;

    public PhotographerReportFacade() {
        super(PhotographerReport.class);
    }

    @Override
    public EntityManager getEm() {
        return em;
    }
}

package pl.lodz.p.it.ssbd2022.ssbd02.mow.service;

import pl.lodz.p.it.ssbd2022.ssbd02.entity.*;
import pl.lodz.p.it.ssbd2022.ssbd02.exceptions.*;
import pl.lodz.p.it.ssbd2022.ssbd02.mow.facade.AccountReportCauseFacade;
import pl.lodz.p.it.ssbd2022.ssbd02.mow.facade.AccountReportFacade;
import pl.lodz.p.it.ssbd2022.ssbd02.mow.facade.MowAccessLevelFacade;

import javax.annotation.security.PermitAll;
import javax.annotation.security.RolesAllowed;
import javax.ejb.Stateless;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.inject.Inject;

import static pl.lodz.p.it.ssbd2022.ssbd02.security.Roles.*;

@Stateless
@TransactionAttribute(TransactionAttributeType.MANDATORY)
public class ReportService {

    @Inject
    private AccountReportCauseFacade accountReportCauseFacade;

    @Inject
    private AccountReportFacade accountReportFacade;

    @Inject
    private MowAccessLevelFacade accessLevelFacade;

    @Inject
    private AccountService accountService;

    @PermitAll
    public AccountReport findAccountReportById(Long id) throws NoAccountReportFoundException {
        throw new UnsupportedOperationException();
    }

    @PermitAll
    public PhotographerReport findPhotographerReportById(Long id) throws NoPhotographerReportFoundException {
        throw new UnsupportedOperationException();
    }

    @PermitAll
    public ReviewReport findReviewReportById(Long id) throws NoReviewReportFoundException {
        throw new UnsupportedOperationException();
    }

    @RolesAllowed(reportClient)
    public void addClientAccountReport(AccountReport report) throws BaseApplicationException {
            if (report.getReported().getLogin().equals(report.getReportee().getLogin())) {
                throw ExceptionFactory.selfReportException();
            }

            AccessLevelValue accessLevelValue = accountService.findAccessLevelValueByName("CLIENT");
            if (accessLevelFacade.getAccessLevelAssignmentForAccount(report.getReported(), accessLevelValue) == null) {
                throw ExceptionFactory.noAccountFound("exception.no_client_found");
            }

            accountReportFacade.persist(report);
    }

    @RolesAllowed(reportPhotographer)
    public void addPhotographerReport(PhotographerReport report) {
        throw new UnsupportedOperationException();
    }

    @RolesAllowed(reportReview)
    public void addReviewReport(ReviewReport report) {
        throw new UnsupportedOperationException();
    }

    @RolesAllowed(listAllReports)
    public void listAllAccountReports() {
        throw new UnsupportedOperationException();
    }

    @RolesAllowed(listAllReports)
    public void listAllPhotographerReports() {
        throw new UnsupportedOperationException();
    }

    @RolesAllowed(listAllReports)
    public void listAllReviewReports() {
        throw new UnsupportedOperationException();
    }

    @RolesAllowed(resolveReport)
    public void resolveReviewReport(ReviewReport report) {
        throw new UnsupportedOperationException();
    }

    @RolesAllowed(resolveReport)
    public void resolvePhotographerReport(PhotographerReport report) {
        throw new UnsupportedOperationException();
    }

    @RolesAllowed(resolveReport)
    public void resolveAccountReport(AccountReport report) {
        throw new UnsupportedOperationException();
    }

    @PermitAll
    public AccountReportCause getAccountReportCause(String cause) throws DataNotFoundException {
        return  accountReportCauseFacade.getAccountReportCause(cause);
    }
}

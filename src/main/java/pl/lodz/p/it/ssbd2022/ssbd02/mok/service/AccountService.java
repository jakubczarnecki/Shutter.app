package pl.lodz.p.it.ssbd2022.ssbd02.mok.service;

import at.favre.lib.crypto.bcrypt.BCrypt;
import pl.lodz.p.it.ssbd2022.ssbd02.entity.AccessLevelAssignment;
import pl.lodz.p.it.ssbd2022.ssbd02.entity.AccessLevelValue;
import pl.lodz.p.it.ssbd2022.ssbd02.entity.Account;
import pl.lodz.p.it.ssbd2022.ssbd02.exceptions.*;
import pl.lodz.p.it.ssbd2022.ssbd02.mok.dto.AccountUpdatePasswordDto;
import pl.lodz.p.it.ssbd2022.ssbd02.mok.dto.EditAccountInfoDto;
import pl.lodz.p.it.ssbd2022.ssbd02.mok.facade.AuthenticationFacade;
import pl.lodz.p.it.ssbd2022.ssbd02.security.AuthenticationContext;

import javax.annotation.security.PermitAll;
import javax.annotation.security.RolesAllowed;
import javax.ejb.Stateless;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.inject.Inject;
import javax.persistence.NoResultException;
import java.util.List;

@Stateless
@TransactionAttribute(TransactionAttributeType.MANDATORY)
public class AccountService {

    @Inject
    private AuthenticationFacade accountFacade;

    @Inject
    private AuthenticationContext authenticationContext;

    /**
     * Zmienie status użytkownika o danym loginie na podany
     *
     * @param login login użytkownika dla którego ma zostać dokonana zmiana statusu
     * @param active status który ma zostać ustawiony
     * @throws NoAccountFound kiedy użytkownik o danym loginie nie zostanie odnaleziony
     * w bazie danych
     */
    @RolesAllowed({"ADMINISTRATOR", "MODERATOR"})
    public void changeAccountStatus(String login, Boolean active) throws NoAccountFound {
        Account account = accountFacade.findByLogin(login);
        account.setActive(active);
        accountFacade.getEm().merge(account); // TODO Po implementacji transakcyjności zmineić na wywołanie metody update fasady
    }

    /**
     * Metoda pozwalająca administratorowi zmienić hasło dowolnego użytkowika
     * @param accountId ID użytkownika, którego hasło administrator chce zmienić
     * @param data obiekt zawierający nowe hasło dla wskazanego użytkownika
     */
    @RolesAllowed({"ADMINISTRATOR"})
    public void changeAccountPasswordAsAdmin(Long accountId, AccountUpdatePasswordDto data) {
        Account target = accountFacade.find(accountId);
        changePassword(target, data.getPassword());
    }

    /**
     * Metoda pozwalająca zmienić własne hasło
     * @param data obiekt zawierający stare hasło (w celu werfyikacji) oraz nowe mające być ustawione dla użytkownika
     */
    @RolesAllowed({"ADMINISTRATOR", "MODERATOR", "PHOTOGRAPHER", "CLIENT"})
    public void updateOwnPassword(AccountUpdatePasswordDto data) throws NoAuthenticatedUserFound {
        if (data.getOldPassword() == null) {
            throw new WrongPasswordException("Old password cannot be null");
        }
        Account current = authenticationContext.getCurrentUsersAccount();
        String oldHash = BCrypt.withDefaults().hashToString(6, data.getOldPassword().toCharArray());
        if (!oldHash.equals(current.getPassword())) {
            throw new PasswordMismatchException();
        }
        changePassword(current, data.getPassword());
    }

    /**
     * Pomocnicza metoda utworzone w celu uniknięcia powtarzania kodu.
     * Zmienia hasło wskazanego użytkownika
     * @param target ID użytkownika, którego modyfikujemy
     * @param newPassword nowe hasło dla użytkownika
     */
    private void changePassword(Account target, String newPassword) {
        if (newPassword.trim().length() < 8) {
            throw new WrongPasswordException("New password cannot be applied");
        }
        String hashed = BCrypt.withDefaults().hashToString(6, newPassword.toCharArray());
        target.setPassword(hashed);
        accountFacade.update(target);
    }

    /**
     * Nadaje lub odbiera wskazany poziom dostępu w obiekcie klasy użytkownika.
     *
     * @param accountId Identyfikator konta użytkownika
     * @param accessLevel Łańcuch znaków zawierający nazwę poziomu dostępu
     * @param active status poziomu dostępu, który ma być ustawiony
     * @throws DataNotFoundException Wyjątek otrzymywany w przypadku próby dokonania operacji na niepoprawnej
     * nazwie poziomu dostępu lub próby ustawienia aktywnego/nieaktywnego już poziomu dostępu
     * @throws CannotChangeException Wyjątek otrzymywany w przypadku próby odebrania poziomu dostępu, którego użytkownik
     * nigdy nie posiadał
     * @see AccountAccessLevelChangeDto
     */
    @RolesAllowed({"ADMINISTRATOR"})
    public void changeAccountAccessLevel(Long accountId, String accessLevel, Boolean active)
            throws DataNotFoundException, CannotChangeException {

        Account target = accountFacade.find(accountId);
        List<AccessLevelAssignment> accountAccessLevels = target.getAccessLevelAssignmentList();
        AccessLevelAssignment accessLevelFound = accountFacade.getAccessLevelAssignmentForAccount(accountId, accessLevel);

        if(accessLevelFound != null) {
            if (accessLevelFound.getActive() == active) {
                throw new CannotChangeException("exception.access_level.already_set");
            }

            accessLevelFound.setActive(active);
        } else {
            AccessLevelValue accessLevelValue = accountFacade.getAccessLevelValue(accessLevel);
            AccessLevelAssignment assignment = new AccessLevelAssignment();

            if (!active) {
                throw new CannotChangeException("exception.access_level.already_false");
            }

            assignment.setLevel(accessLevelValue);
            assignment.setAccount(target);
            assignment.setActive(active);
            accountAccessLevels.add(assignment);

            target.setAccessLevelAssignmentList(accountAccessLevels);
        }

        accountFacade.update(target);
    }

    /**
     * Rejestruje konto użytkownika z danych podanych w obiekcie klasy użytkownika
     * oraz przypisuje do niego poziom dostępu klienta.
     * W celu aktywowania konta należy jeszcze zmienić pole 'registered' na wartość 'true'
     *
     * @param account Obiekt klasy Account reprezentującej dane użytkownika
     * @throws BaseApplicationException Wyjątek otrzymywany w przypadku niepowodzenia rejestracji (login lub adres email już istnieje)
     * @see Account
     */
    @PermitAll
    public void registerAccount(Account account) throws BaseApplicationException {

        account.setPassword(BCrypt.withDefaults().hashToString(6, account.getPassword().toCharArray()));
        account.setActive(true);
        account.setRegistered(false);

        AccessLevelValue levelValue = accountFacade.getAccessLevelValue("CLIENT");

        AccessLevelAssignment assignment = new AccessLevelAssignment();
        assignment.setLevel(levelValue);
        assignment.setAccount(account);
        assignment.setActive(true);

        List<AccessLevelAssignment> list = account.getAccessLevelAssignmentList();
        list.add(assignment);

        account.setAccessLevelAssignmentList(list);

        accountFacade.registerAccount(account);
    }

    /**
     * Funckja do edycji danych użytkownika. Zmienia tylko proste informacje a nie role dostępu itp
     *
     * @param editAccountInfoDto klasa zawierająca zmienione dane danego użytkownika
     * @return obiekt użytkownika po aktualizacji
     * @throws NoAuthenticatedUserFound W przypadku gdy nie znaleziono aktualnego użytkownika
     */
    @RolesAllowed({"ADMINISTRATOR", "MODERATOR", "PHOTOGRAPHER", "CLIENT"})
    public Account editAccountInfo(EditAccountInfoDto editAccountInfoDto) throws NoAuthenticatedUserFound {
        Account account = authenticationContext.getCurrentUsersAccount();
        account.setEmail(editAccountInfoDto.getEmail());
        account.setName(editAccountInfoDto.getName());
        account.setSurname(editAccountInfoDto.getSurname());
        return accountFacade.update(account);
    }
}

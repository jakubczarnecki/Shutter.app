package pl.lodz.p.it.ssbd2022.ssbd02.mok.service;

import org.hibernate.exception.ConstraintViolationException;
import pl.lodz.p.it.ssbd2022.ssbd02.entity.AccessLevelAssignment;
import pl.lodz.p.it.ssbd2022.ssbd02.entity.AccessLevelValue;
import pl.lodz.p.it.ssbd2022.ssbd02.entity.Account;
import pl.lodz.p.it.ssbd2022.ssbd02.exceptions.*;
import pl.lodz.p.it.ssbd2022.ssbd02.mok.dto.*;
import pl.lodz.p.it.ssbd2022.ssbd02.mok.facade.AccessLevelFacade;
import pl.lodz.p.it.ssbd2022.ssbd02.mok.facade.AuthenticationFacade;
import pl.lodz.p.it.ssbd2022.ssbd02.security.BCryptUtils;
import pl.lodz.p.it.ssbd2022.ssbd02.util.EmailService;
import pl.lodz.p.it.ssbd2022.ssbd02.util.LoggingInterceptor;

import javax.annotation.security.PermitAll;
import javax.annotation.security.RolesAllowed;
import javax.ejb.Stateless;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.inject.Inject;
import javax.interceptor.Interceptors;
import javax.persistence.PersistenceException;
import java.util.List;
import java.util.stream.Collectors;

import static pl.lodz.p.it.ssbd2022.ssbd02.security.Roles.*;
import static pl.lodz.p.it.ssbd2022.ssbd02.util.ConstraintNames.IDENTICAL_EMAIL;
import static pl.lodz.p.it.ssbd2022.ssbd02.util.ConstraintNames.IDENTICAL_LOGIN;

@Stateless
@Interceptors(LoggingInterceptor.class)
@TransactionAttribute(TransactionAttributeType.MANDATORY)
public class AccountService {

    @Inject
    private AuthenticationFacade accountFacade;

    @Inject
    private AccessLevelFacade accessLevelFacade;

    @Inject
    private VerificationTokenService verificationTokenService;

    @Inject
    private EmailService emailService;

    /**
     * Odnajduje konto użytkownika o podanym loginie
     *
     * @param login Login użytkownika, którego konta ma być wyszukane
     * @throws NoAccountFound W przypadku nieznalezienia konta
     */
    @PermitAll
    public Account findByLogin(String login) throws  NoAccountFound {
        return accountFacade.findByLogin(login);
    }

    /**
     * Odnajduje wybraną wartość poziomu dostępu na bazie jej nazwy
     *
     * @param name Nazwa poziomu dostępu
     * @throws DataNotFoundException W momencie, gdy dany poziom dostępu nie zostanie odnaleziony
     */
    @PermitAll
    public AccessLevelValue findAccessLevelValueByName(String name) throws DataNotFoundException {
        return accessLevelFacade.getAccessLevelValue(name);
    }

    /**
     * Zmienia status użytkownika o danym loginie na podany
     *
     * @param account użytkownika, dla którego ma zostać dokonana zmiana statusu
     * @param active  status, który ma zostać ustawiony
     */
    @RolesAllowed({blockAccount, unblockAccount})
    public void changeAccountStatus(Account account, Boolean active) {
        account.setActive(active);
        accountFacade.update(account);
    }

    /**
     * Szuka użytkownika
     *
     * @param requester konto użytkownika, który chce uzyskać informacje o danym koncie
     * @param account konto użytkownika, którego dane mają zostać pozyskane
     * @return obiekt DTO informacji o użytkowniku
     * @throws NoAccountFound              W przypadku gdy użytkownik o podanej nazwie nie istnieje lub
     *                                     gdy konto szukanego użytkownika jest nieaktywne, lub niepotwierdzone
     *                                     i informacje próbuje uzyskać użytkownik niebędący ani administratorem,
     *                                     ani moderatorem
     * @see AccountInfoDto
     */
    @RolesAllowed({ADMINISTRATOR, MODERATOR, PHOTOGRAPHER, CLIENT})
    public Account getAccountInfo(Account requester, Account account) throws NoAccountFound {
        List<String> accessLevelList = requester
                .getAccessLevelAssignmentList()
                .stream()
                .filter(AccessLevelAssignment::getActive)
                .map(a -> a.getLevel().getName())
                .collect(Collectors.toList());
        if (Boolean.TRUE.equals(account.getActive()) && Boolean.TRUE.equals(account.getRegistered())) {
            return account;
        }
        if (accessLevelList.contains(ADMINISTRATOR) || accessLevelList.contains(MODERATOR)) {
            return account;
        }
        throw ExceptionFactory.noAccountFound();
    }

    /**
     * Metoda pozwalająca administratorowi zmienić hasło dowolnego użytkowika
     *
     * @param account   Użytkownik, którego hasło administrator chce zmienić
     * @param password  Nowe hasło dla wskazanego użytkownika
     */
    @RolesAllowed(changeSomeonesPassword)
    public void changeAccountPasswordAsAdmin(Account account, String password) {
        changePassword(account, password);
    }

    /**
     * Metoda pozwalająca zmienić własne hasło
     *
     * @param data obiekt zawierający stare hasło (w celu weryfikacji) oraz nowe mające być ustawione dla użytkownika
     */
    @RolesAllowed(changeOwnPassword)
    public void updateOwnPassword(Account account, AccountUpdatePasswordDto data) throws PasswordMismatchException {
        if (data.getOldPassword() == null) {
            throw ExceptionFactory.wrongPasswordException();
        }
        if (!BCryptUtils.verify(data.getOldPassword().toCharArray(), account.getPassword())) {
            throw ExceptionFactory.passwordMismatchException();
        }

        changePassword(account, data.getPassword());
    }

    /**
     * Pomocnicza metoda utworzone w celu uniknięcia powtarzania kodu.
     * Zmienia hasło wskazanego użytkownika
     *
     * @param target      ID użytkownika, którego modyfikujemy
     * @param newPassword nowe hasło dla użytkownika
     */
    private void changePassword(Account target, String newPassword) {
        if (newPassword.trim().length() < 8) {
            throw ExceptionFactory.wrongPasswordException();
        }
        String hashed = BCryptUtils.generate(newPassword.toCharArray());
        target.setPassword(hashed);
        accountFacade.update(target);
    }

    /**
     * Resetuje hasło użytkownika na podane pod warunkiem, że żeton weryfikujący jest aktualny oraz poprawny
     *
     * @param account          Konto, dla którego hasła ma zostać zresetowane
     * @param resetPasswordDto Dto przechowujące informacje wymagane do resetu hasła
     * @throws InvalidTokenException    Kiedy żeton jest nieprawidłowy
     * @throws NoVerificationTokenFound Kiedy nie udało się odnaleźć danego żetonu w systemie
     * @throws ExpiredTokenException    Kiedy żeton wygasł
     */
    @PermitAll
    public void resetPassword(Account account, ResetPasswordDto resetPasswordDto) throws InvalidTokenException, NoVerificationTokenFound, ExpiredTokenException {
        verificationTokenService.confirmPasswordReset(resetPasswordDto.getToken());
        changePassword(account, resetPasswordDto.getNewPassword());
    }

    /**
     * Nadaje lub odbiera wskazany poziom dostępu w obiekcie klasy użytkownika.
     *
     * @param account                   Konto użytkownika, dla którego ma nastąpić zmiana poziomu dostępu
     * @param accessLevelValue          Poziom dostępu który ma zostać zmieniony dla użytkownika
     * @param active                    Status poziomu dostępu, który ma być ustawiony
     * @throws CannotChangeException    W przypadku próby odebrania poziomu dostępu, którego użytkownik nigdy nie posiadał
     * @see AccountAccessLevelChangeDto
     */
    @RolesAllowed({ADMINISTRATOR})
    public void changeAccountAccessLevel(Account account, AccessLevelValue accessLevelValue, Boolean active)
            throws CannotChangeException {

        List<AccessLevelAssignment> accountAccessLevels = account.getAccessLevelAssignmentList();
        AccessLevelAssignment accessLevelFound = accessLevelFacade.getAccessLevelAssignmentForAccount(
                account,
                accessLevelValue
        );

        if(accessLevelFound != null) {
            if (accessLevelFound.getActive() == active) {
                throw new CannotChangeException("exception.access_level.already_set");
            }

            accessLevelFound.setActive(active);
            accessLevelFacade.update(accessLevelFound);
        } else {
            AccessLevelAssignment assignment = new AccessLevelAssignment();

            if (!active) {
                throw new CannotChangeException("exception.access_level.already_false");
            }

            assignment.setLevel(accessLevelValue);
            assignment.setAccount(account);
            assignment.setActive(active);

            accessLevelFacade.persist(assignment);
        }
    }

    /**
     * Ustawia poziom dostępu fotografa w obiekcie klasy użytkownika na aktywny.
     *
     * @param account                   Konto użytkownika, dla którego ma nastąpić nadanie roli fotografa
     * @throws CannotChangeException    W przypadku próby zostania fotografem przez uzytkownika mającego już tę rolę
     * @see AccountAccessLevelChangeDto
     */
    @RolesAllowed({becomePhotographer})
    public void becomePhotographer(Account account)
            throws CannotChangeException, DataNotFoundException {

        AccessLevelAssignment accessLevelFound = accessLevelFacade.getAccessLevelAssignmentForAccount(
                account,
                accessLevelFacade.getAccessLevelValue("PHOTOGRAPHER")
        );

        if (accessLevelFound != null) {
            if (accessLevelFound.getActive()) {
                throw new CannotChangeException("exception.access_level.already_set");
            }

            accessLevelFound.setActive(true);
            accessLevelFacade.update(accessLevelFound);
        } else {
            AccessLevelAssignment assignment = new AccessLevelAssignment();

            assignment.setLevel(accessLevelFacade.getAccessLevelValue("PHOTOGRAPHER"));
            assignment.setAccount(account);
            assignment.setActive(true);

            accessLevelFacade.persist(assignment);
        }
    }

    /**
     * Rejestruje konto użytkownika z danych podanych w obiekcie klasy użytkownika
     * oraz przypisuje do niego poziom dostępu klienta.
     * W celu aktywowania konta należy jeszcze zmienić pole 'registered' na wartość 'true'.
     *
     * @param account Obiekt klasy Account reprezentującej dane użytkownika
     * @throws IdenticalFieldException Wyjątek otrzymywany w przypadku niepowodzenia rejestracji
     *                                 (login lub adres email już istnieje)
     * @see Account
     */
    @PermitAll
    public void registerOwnAccount(Account account)
            throws IdenticalFieldException, DataNotFoundException, DatabaseException {
        account.setPassword(BCryptUtils.generate(account.getPassword().toCharArray()));
        account.setActive(true);
        account.setRegistered(false);
        account.setFailedLogInAttempts(0);

        addNewAccount(account);

        addClientAccessLevel(account);

        verificationTokenService.sendRegistrationToken(account);
    }

    /**
     * Rejestruje konto użytkownika z danych podanych w obiekcie klasy użytkownika (wraz z polami registered i active)
     * oraz przypisuje do niego poziom dostępu klienta.
     *
     * @param account Obiekt klasy Account reprezentującej dane użytkownika
     * @throws IdenticalFieldException Wyjątek otrzymywany w przypadku niepowodzenia rejestracji
     *                                 (login lub adres email już istnieje)
     * @see Account
     */
    @RolesAllowed({ADMINISTRATOR})
    public void registerAccountByAdmin(Account account)
            throws IdenticalFieldException, DatabaseException, DataNotFoundException {
        account.setPassword(BCryptUtils.generate(account.getPassword().toCharArray()));
        account.setFailedLogInAttempts(0);

        addNewAccount(account);

        addClientAccessLevel(account);

        if (!account.getRegistered()) {
            verificationTokenService.sendRegistrationToken(account);
        }
    }

    /**
     * Tworzy konto użytkownika w bazie danych,
     * w przypadku naruszenia unikatowości loginu lub adresu email otrzymujemy wyjątek
     *
     * @param account obiekt encji użytkownika
     * @throws IdenticalFieldException W przypadku, gdy login lub adres email już się znajduje w bazie danych
     */
    private void addNewAccount(Account account) throws IdenticalFieldException, DatabaseException {
        try {
            accountFacade.persist(account);
        } catch (PersistenceException ex) {
            if (ex.getCause() instanceof ConstraintViolationException) {
                String name = ((ConstraintViolationException) ex.getCause()).getConstraintName();
                switch (name) {
                    case IDENTICAL_LOGIN:
                        throw ExceptionFactory.identicalFieldException("exception.login.identical");
                    case IDENTICAL_EMAIL:
                        throw ExceptionFactory.identicalFieldException("exception.email.identical");
                }
            }
            throw ExceptionFactory.databaseException();
        }
    }

    /**
     * Metoda pomocnicza tworząca wpis o poziomie dostępu klient dla danego użytkownika.
     *
     * @param account Obiekt klasy Account reprezentującej dane użytkownika
     */
    private void addClientAccessLevel(Account account) throws DataNotFoundException {
        AccessLevelValue levelValue = accessLevelFacade.getAccessLevelValue(CLIENT);

        AccessLevelAssignment assignment = new AccessLevelAssignment();

        assignment.setLevel(levelValue);
        assignment.setAccount(account);
        assignment.setActive(true);

        accessLevelFacade.persist(assignment);
    }

    /**
     * Potwierdza rejestracje konta ustawiając pole 'registered' na wartość 'true'
     *
     * @param token Obiekt przedstawiający żeton weryfikacyjny użyty do potwierdzenia rejestracji
     * @throws BaseApplicationException Występuje w przypadku gdy potwierdzenie rejestracji się nie powiedzie
     */
    @PermitAll
    public void confirmAccountRegistration(String token) throws BaseApplicationException {
        Account account = verificationTokenService.confirmRegistration(token);
        account.setRegistered(true);
        accountFacade.update(account);
    }

    /**
     * Funkcja do edycji danych użytkownika. Zmienia tylko proste informacje, a nie role dostępu itp
     *
     * @param editAccountInfoDto klasa zawierająca zmienione dane danego użytkownika
     */
    @RolesAllowed(editOwnAccountData)
    public void editAccountInfo(Account account, EditAccountInfoDto editAccountInfoDto) {
        account.setEmail(editAccountInfoDto.getEmail());
        account.setName(editAccountInfoDto.getName());
        account.setSurname(editAccountInfoDto.getSurname());
        accountFacade.update(account);
    }

    /**
     * Funkcja do edycji danych innego użytkownika przez Administratora. Pozwala zmienić jedynie email,
     * imię oraz nazwisko
     *
     * @param editAccountInfoDto klasa zawierająca zmienione dane danego użytkownika
     */
    @RolesAllowed({ADMINISTRATOR})
    public void editAccountInfoAsAdmin(Account account, EditAccountInfoDto editAccountInfoDto) {
        account.setEmail(editAccountInfoDto.getEmail());
        account.setName(editAccountInfoDto.getName());
        account.setSurname(editAccountInfoDto.getSurname());
        accountFacade.update(account);
    }

    /**
     * Rejestruje nieudane logowanie na konto użytkownika poprzez inkrementację licznika nieudanych
     * logowań jego konta. Jeżeli liczba nieudanych logowań będzie równa lub większa od 3, to konto zostaje
     * automatycznie zablokowane, a użytkownik zostaje powiadomiony o tym drogą mailową.
     *
     * @param account Konto, dla którego należy zarejestrować nieudaną operację logowania
     */
    @PermitAll
    public void registerFailedLogInAttempt(Account account) {
        if (!account.getActive() || !account.getRegistered()) return;

        Integer failedAttempts = account.getFailedLogInAttempts();
        failedAttempts++;
        account.setFailedLogInAttempts(failedAttempts);

        if (failedAttempts >= 3) {
            account.setActive(false);
            account.setFailedLogInAttempts(0);
            emailService.sendAccountBlockedDueToToManyLogInAttemptsEmail(account.getEmail());
        }
    }

    /**
     * Rejestruje udane logowanie na konto użytkownika poprzez wyzerowanie licznika nieudanych zalogowań.
     *
     * @param account Konto, dla którego należy wyzerować licznik nieudanych logowań
     */
    @PermitAll
    public void registerSuccessfulLogInAttempt(Account account) {
        if (!account.getActive() || !account.getRegistered()) return;
        account.setFailedLogInAttempts(0);
    }
}
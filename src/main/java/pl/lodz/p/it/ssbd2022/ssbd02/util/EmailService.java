package pl.lodz.p.it.ssbd2022.ssbd02.util;

import pl.lodz.p.it.ssbd2022.ssbd02.entity.VerificationToken;
import pl.lodz.p.it.ssbd2022.ssbd02.exceptions.EmailException;
import pl.lodz.p.it.ssbd2022.ssbd02.exceptions.ExceptionFactory;

import javax.annotation.PostConstruct;
import javax.annotation.security.PermitAll;
import javax.annotation.security.RolesAllowed;
import javax.ejb.Stateless;
import javax.inject.Inject;
import javax.interceptor.Interceptors;
import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;
import java.util.Locale;
import java.util.Properties;

import static pl.lodz.p.it.ssbd2022.ssbd02.security.Roles.*;
import static pl.lodz.p.it.ssbd2022.ssbd02.util.I18n.*;

/**
 * Klasa służąca do wysyłania maili
 */
@Stateless
@Interceptors(LoggingInterceptor.class)
public class EmailService {

    private String fromEmail;
    private Session session;

    @Inject
    private ConfigLoader configLoader;

    @Inject
    private I18n i18n;


    @PostConstruct
    public void init() {
        fromEmail = configLoader.getEmailSenderAddress();
        Properties properties = System.getProperties();
        properties.put("mail.smtp.host", configLoader.getMailSmtpHost());
        properties.put("mail.smtp.port", configLoader.getMailSmtpPort());
        properties.put("mail.smtp.starttls.enable", configLoader.getMailSsl());
        properties.put("mail.smtp.auth", configLoader.getMailSmtpAuth());

        session = Session.getInstance(properties, new javax.mail.Authenticator() {
            @Override
            protected javax.mail.PasswordAuthentication getPasswordAuthentication() {
                return new javax.mail.PasswordAuthentication(configLoader.getMailSmtpUser(), configLoader.getMailSmtpPassword());
            }
        });

    }

    /**
     * Przykładowa funkcja korzystająca z funkcji sendMail
     *
     * @param to    adresat wiadomości email
     * @param token Obiekt przedstawiający żeton weryfikacyjny użyty do potwierdzenia rejestracji
     */
    @PermitAll
    public void sendRegistrationEmail(String to, Locale locale, VerificationToken token) {
        String subject = i18n.getMessage(REGISTRATION_CONFIRMATION_SUBJECT, locale);
        String body = i18n.getMessage(REGISTRATION_CONFIRMATION_BODY, locale) + String.format(
                "%s/confirm-registration/%s",
                configLoader.getEmailAppUrl(),
                token.getToken()
        );

        try {
            sendEmail(to, subject, body);
        } catch (EmailException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Funkcja wysyłająca do wskazanego użytkownika maila z przypomnieniem o konieczności potwierdzenia rejestracji
     *
     * @param to    adresat wiadomości email
     * @param token Obiekt przedstawiający żeton weryfikacyjny użyty do potwierdzenia rejestracji
     */
    @PermitAll
    public void sendRegistrationConfirmationReminder(String to, Locale locale, VerificationToken token) {
        String subject = i18n.getMessage(REGISTRATION_CONFIRMATION_REMINDER_SUBJECT, locale);
        String body = i18n.getMessage(REGISTRATION_CONFIRMATION_REMINDER_BODY, locale) + String.format(
                "%s/confirm-registration/%s",
                configLoader.getEmailAppUrl(),
                token.getToken()
        );

        try {
            sendEmail(to, subject, body);
        } catch (EmailException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Wysyła na adres email podany jako parametr żeton weryfikacyjny resetowania hasła
     *
     * @param to    Adres e-mail, na który wysłany ma zostać wiadomość zawierająca żeton
     * @param token Żeton, który ma zostać wysłany
     */
    @PermitAll
    public void sendPasswordResetEmail(String to, Locale locale, VerificationToken token) {
        String subject = i18n.getMessage(PASSWORD_RESET_SUBJECT, locale);
        String body = i18n.getMessage(PASSWORD_RESET_BODY, locale) + String.format(
                "%s/password-reset/%s",
                configLoader.getEmailAppUrl(),
                token.getToken()
        );

        try {
            sendEmail(to, subject, body);
        } catch (EmailException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Wysyła na adres email podany jako parametr żeton weryfikacyjny resetowania hasła w przypadku kiedy
     * zostanie ono zmienione przez administratora systemu
     *
     * @param to    Adres e-mail, na który wysłany ma zostać wiadomość zawierająca żeton
     * @param token Żeton, który ma zostać wysłany
     */
    @RolesAllowed(changeSomeonesPassword)
    public void sendForcedPasswordResetEmail(String to, Locale locale, VerificationToken token) {
        String subject = i18n.getMessage(FORCED_PASSWORD_RESET_SUBJECT, locale);
        String body = i18n.getMessage(FORCED_PASSWORD_RESET_REMINDER_BODY, locale) + String.format(
                "%s/password-reset/%s",
                configLoader.getEmailAppUrl(),
                token.getToken()
        );

        try {
            sendEmail(to, subject, body);
        } catch (EmailException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Wysyła email powiadamiający użytkownika o zablokowaniu jego konta z powodu zbyt wielu nieudanych
     * prób logowania
     *
     * @param to Adres e-mail, na który należy wysłać wiadomość
     */
    @PermitAll
    public void sendAccountBlockedDueToToManyLogInAttemptsEmail(String to, Locale locale) {
        String subject = i18n.getMessage(INACTIVE_ACCOUNT_BLOCK_SUBJECT, locale);
        String body = i18n.getMessage(INACTIVE_ACCOUNT_BLOCK_BODY, locale);

        try {
            sendEmail(to, subject, body);
        } catch (EmailException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Wysyła na podany adres email ostrzeżenie o logowaniu na konto administratora
     *
     * @param to        Email, na który zostać ma wysłane powiadomienie. Powinien być to email administratora systemu
     * @param login     Login użytkownika, na którego email ma zostać przesłane powiadomienie
     * @param ipAddress adres IP, z którego dokonano logowania na konto administratora
     */
    @PermitAll
    public void sendAdminAuthenticationWaringEmail(String to, Locale locale, String login, String ipAddress) {
        String subject = i18n.getMessage(ADMIN_AUTHENTICATION_WARNING_SUBJECT, locale);
        String body = String.format(
                i18n.getMessage(ADMIN_AUTHENTICATION_WARNING_BODY, locale),
                login,
                ipAddress
        );

        try {
            sendEmail(to, subject, body);
        } catch (EmailException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Wysyła na adres email podany jako parametr żeton weryfikacyjny do aktualizacji adresu email
     *
     * @param to    Adres e-mail, na który wysłany ma zostać wiadomość zawierająca żeton
     * @param token Żeton, który ma zostać wysłany
     */
    @RolesAllowed(updateEmail)
    public void sendEmailUpdateEmail(String to, Locale locale, VerificationToken token) {
        String subject = i18n.getMessage(EMAIL_UPDATE_SUBJECT, locale);
        String body = i18n.getMessage(EMAIL_UPDATE_BODY, locale) + String.format(
                "%s/change-own-email/%s",
                configLoader.getEmailAppUrl(),
                token.getToken()
        );

        try {
            sendEmail(to, subject, body);
        } catch (EmailException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Funkcja wysyłająca do wskazanego użytkownika maila z przypomnieniem o żądaniu zmiany maila konta
     *
     * @param to    adresat wiadomości email
     * @param token Obiekt przedstawiający żeton weryfikacyjny użyty do zmiany maila
     */
    @PermitAll
    public void sendEmailResetReminderEmail(String to, Locale locale, VerificationToken token) {
        String subject = i18n.getMessage(EMAIL_UPDATE_REMINDER_SUBJECT, locale);
        String body = i18n.getMessage(EMAIL_UPDATE_REMINDER_BODY, locale) + String.format(
                "%s/change-own-email/%s",
                configLoader.getEmailAppUrl(),
                token.getToken()
        );

        try {
            sendEmail(to, subject, body);
        } catch (EmailException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Wysyła na adres email podany jako parametr kod 2FA
     *
     * @param to   Adres e-mail, na który wysłany ma zostać wiadomość zawierająca kod 2FA
     * @param name Imię użytkownika, do którego jest wysyłany kod
     * @param code Kod 2FA
     */
    @PermitAll
    public void sendEmail2FA(String to, Locale locale, String name, String code) {
        String subject = i18n.getMessage(TWO_FA_SUBJECT, locale);
        String body =
                name.substring(0, 1).toUpperCase() +
                        name.substring(1) +
                        i18n.getMessage(TWO_FA_BODY, locale) +
                        code;

        try {
            sendEmail(to, subject, body);
        } catch (EmailException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Wysyła na adres email podany jako parametr link do aktywacji konta po jego zablokowaniu
     *
     * @param to    Adres e-mail, na który wysłana ma zostać wiadomość
     * @param token Żeton, który ma zostać wysłany
     */
    @PermitAll
    public void sendEmailUnblockAccount(String to, Locale locale, VerificationToken token) {
        String subject = i18n.getMessage(UNBLOCK_ACCOUNT_SUBJECT, locale);
        String body = i18n.getMessage(UNBLOCK_ACCOUNT_BODY, locale) + String.format(
                "%s/unblock-account/%s",
                configLoader.getEmailAppUrl(),
                token.getToken()
        );

        try {
            sendEmail(to, subject, body);
        } catch (EmailException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Funkcja wysyłająca na podany adres email informację o tym, że konto użytkownika z nim powiązane zostało
     * odblokowane w systemie.
     *
     * @param to adresat wiadomości email
     */
    @RolesAllowed(unblockAccount)
    public void sendAccountUnblockedEmail(String to, Locale locale) {
        String subject = i18n.getMessage(ACCOUNT_UNBLOCKED_SUBJECT, locale);
        String body = i18n.getMessage(ACCOUNT_UNBLOCKED_BODY, locale);

        try {
            sendEmail(to, subject, body);
        } catch (EmailException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Funkcja wysyłająca na podany adres email informację o tym, że konto użytkownika z nim powiązane zostało
     * zablokowane w systemie.
     *
     * @param to adresat wiadomości email
     */
    @RolesAllowed(blockAccount)
    public void sendAccountBlocked(String to, Locale locale) {
        String subject = i18n.getMessage(ACCOUNT_BLOCKED_SUBJECT, locale);
        String body = i18n.getMessage(ACCOUNT_BLOCKED_BODY, locale);

        try {
            sendEmail(to, subject, body);
        } catch (EmailException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Funkcja wysyłająca na podany adres email informację o tym, że danemu użytkownikowi o tym adresie
     * email został przypisany nowy poziom dostępu.
     *
     * @param to              adresat wiadomości email
     * @param accessLevelName nazwa poziomu dostępu, który został przypisany
     */
    @PermitAll
    public void sendAccessLevelGrantedEmail(String to, Locale locale, String accessLevelName) {
        String subject = i18n.getMessage(ACCESS_LEVEL_GRANTED_SUBJECT, locale);
        String body = i18n.getMessage(ACCESS_LEVEL_GRANTED_BODY, locale) + accessLevelName + ".";

        try {
            sendEmail(to, subject, body);
        } catch (EmailException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Funkcja wysyłająca na podany adres email informację o tym, że danemu użytkownikowi o tym adresie
     * email został odebrany poziom dostępu
     *
     * @param to              adresat wiadomości email
     * @param accessLevelName nazwa poziomu dostępu, który został odebrany
     */
    @RolesAllowed(revokeAccessLevel)
    public void sendAccessLevelRevokedEmail(String to, Locale locale, String accessLevelName) {
        String subject = i18n.getMessage(ACCESS_LEVEL_REVOKED_SUBJECT, locale);
        String body = i18n.getMessage(ACCESS_LEVEL_REVOKED_BODY, locale) + accessLevelName + ".";

        try {
            sendEmail(to, subject, body);
        } catch (EmailException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Funkcja wysyłająca na podany adres email informację o tym, że konto użytkownika z nim powiązane zostało
     * aktywowane w systemie.
     *
     * @param to adresat wiadomości email
     */
    @PermitAll
    public void sendAccountActivated(String to, Locale locale) {
        String subject = i18n.getMessage(ACCOUNT_ACTIVATED_SUBJECT, locale);
        String body = i18n.getMessage(ACCOUNT_ACTIVATED_BODY, locale);

        try {
            sendEmail(to, subject, body);
        } catch (EmailException e) {
            throw new RuntimeException(e);
        }
    }


    /**
     * Funkcja wysyłająca email informujący fotografa o odwołaniu rezerwacji przez jego klienta
     *
     * @param to            adres email fotografa
     * @param reservationId id odwołanej rezerwacji
     * @param locale        parametr określajacy język, w jakim wiadomość będzie wysłana
     */
    public void sendReservationCanceledEmail(String to, Long reservationId, Locale locale) {
        String subject = i18n.getMessage(RESERVATION_CANCELED, locale);
        String body = i18n.getMessage(RESERVATION_CANCELED_BODY, locale) + reservationId + ".";

        try {
            sendEmail(to, subject, body);
        } catch (EmailException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Funkcja wysyłająca email informujący klienta o odrzuceniu rezerwacji przez fotografa
     *
     * @param to            adres email klienta
     * @param reservationId id odwołanej rezerwacji
     * @param locale        parametr określajacy język, w jakim wiadomość będzie wysłana
     */
    public void sendReservationDiscardedEmail(String to, Long reservationId, Locale locale) {
        String subject = i18n.getMessage(RESERVATION_DISCARDED, locale);
        String body = i18n.getMessage(RESERVATION_DISCARDED_BODY, locale) + reservationId + ".";

        try {
            sendEmail(to, subject, body);
        } catch (EmailException e) {
            throw new RuntimeException(e);
        }
    }


    /**
     * Funkcja służąca do wysyłania emaili
     *
     * @param toEmail  adres email odbiorcy
     * @param subject  tytuł emaila
     * @param bodyText treść emaila w html
     */
    @PermitAll
    public void sendEmail(String toEmail,
                          String subject,
                          String bodyText
    ) throws EmailException {

        try {
            MimeMessage message = new MimeMessage(session);
            message.setFrom(new InternetAddress(fromEmail));
            message.addRecipient(Message.RecipientType.TO, new InternetAddress(toEmail));
            message.setSubject(subject);
            message.setContent("<html> <head> <style> body { font-family: 'Nunito', sans-serif; color: #7143d2; background-color: #eeeeee; padding-top: 30px; padding-bottom: 30px; width: 100%; height: max-content; } .card { padding: 20px; padding-bottom: 10px; border-radius: 10px; box-shadow: 6px 6px 16px 0 #dbdbdb80, -6px -6px 16px 0 #fbfbfb80; background-color: white; width: 50%; margin: 0 auto; margin-top: 100px; height: fit-content; } img { width: 50px; height: 50px; display: block; margin-left: auto; margin-right: auto; margin-bottom: -10px; } h1 { text-align: center; } p { margin-left: auto; margin-right: auto; } .content { color: black; text-align: left; max-width: 70%; margin: 0 auto; } .divisor { width: 75%; margin-left: 12.5%; background-color: #969696; height: 1px; } #footer { padding-top: 20px; padding-bottom: 10px; } .footer { width: 100%; font-weight: bold; text-align: center; padding-top: 10px; height: 60px; } </style> </head> <body> <div class='card'> <div> <img src='https://ssbd02.s3.eu-central-1.amazonaws.com/logo.png'/> <h1>SHUTTER.APP</h1> </div> <div class='content'> <p>" +
                            bodyText +
                            "</p> </div> <div class='footer'> <div class='divisor'/> <p id='footer'>ⓒ 2022 SHUTTER.APP | SSBD202202</p> </div> </div> </body></html>"
                    , "text/html");
            Transport.send(message);
        } catch (MessagingException e) {
            throw ExceptionFactory.emailException(e.getMessage());
        }
    }
}
package pl.lodz.p.it.ssbd2022.ssbd02.security;

import com.bastiaanjansen.otp.HMACAlgorithm;
import com.bastiaanjansen.otp.TOTP;
import pl.lodz.p.it.ssbd2022.ssbd02.exceptions.NoConfigFileFound;
import pl.lodz.p.it.ssbd2022.ssbd02.util.ConfigLoader;
import pl.lodz.p.it.ssbd2022.ssbd02.util.LoggingInterceptor;

import javax.annotation.PostConstruct;
import javax.ejb.Stateless;
import javax.inject.Inject;
import javax.interceptor.Interceptors;
import java.nio.charset.StandardCharsets;
import java.time.Duration;

@Stateless
@Interceptors({LoggingInterceptor.class})
public class OneTimeCodeUtils {
    @Inject
    private ConfigLoader configLoader;
    private byte[] secret;
    private TOTP totpGenerator;

    public void setConfigLoader(ConfigLoader configLoader) {
        this.configLoader = configLoader;
    }

    @PostConstruct
    public void init() {
        String period = "600";
        try {
            this.secret = configLoader
                    .loadProperties("config.2fa.properties")
                    .getProperty("2fa.secret")
                    .getBytes(StandardCharsets.UTF_8);
            period = configLoader
                    .loadProperties("config.2fa.properties")
                    .getProperty("2fa.period");
        } catch (NoConfigFileFound e) {
            throw new RuntimeException(e);
        }
        TOTP.Builder builder = new TOTP.Builder(secret);
        this.totpGenerator = builder
                .withPasswordLength(6)
                .withAlgorithm(HMACAlgorithm.SHA512)
                .withPeriod(Duration.ofSeconds(Integer.parseInt(period)))
                .build();
    }

    public String generateCode() {
        return totpGenerator.now();
    }
    public boolean verifyCode(String code) {
        return totpGenerator.verify(code);
    }
}

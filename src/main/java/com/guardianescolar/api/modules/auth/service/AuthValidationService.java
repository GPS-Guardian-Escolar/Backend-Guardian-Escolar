package com.guardianescolar.api.modules.auth.service;

import com.guardianescolar.api.modules.security.domain.TwoFactorMethods;
import com.guardianescolar.api.modules.security.domain.User;
import com.guardianescolar.api.modules.security.repository.UserRepository;
import java.util.Hashtable;
import java.util.Set;
import java.util.regex.Pattern;
import javax.naming.NamingException;
import javax.naming.directory.Attributes;
import javax.naming.directory.InitialDirContext;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AuthValidationService {

    private static final Pattern HAS_UPPERCASE = Pattern.compile(".*[A-Z].*");
    private static final Pattern HAS_LOWERCASE = Pattern.compile(".*[a-z].*");
    private static final Pattern HAS_NUMBER = Pattern.compile(".*\\d.*");
    private static final Pattern HAS_SYMBOL = Pattern.compile(".*[^a-zA-Z0-9].*");
    private static final Pattern STRICT_EMAIL = Pattern.compile(
            "^[a-z0-9](?:[a-z0-9._%+-]{0,62}[a-z0-9])?@(?:[a-z0-9](?:[a-z0-9-]{0,61}[a-z0-9])?\\.)+[a-z]{2,24}$");
    private static final Set<String> SUPPORTED_EMAIL_TLDS = Set.of(
            "com", "co", "edu", "org", "net", "gov", "mil", "info", "io", "app", "dev", "es");
    private static final int PASSWORD_MIN_LENGTH = 8;
    private static final int MAX_USERS_PER_PHONE = 3;

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public void validatePasswordPolicy(String password) {
        if (password.length() < PASSWORD_MIN_LENGTH || password.length() > 128) {
            throw new IllegalArgumentException("La contraseña debe tener entre 8 y 128 caracteres");
        }
        if (!HAS_UPPERCASE.matcher(password).matches()) {
            throw new IllegalArgumentException("La contraseña debe incluir al menos una mayúscula");
        }
        if (!HAS_LOWERCASE.matcher(password).matches()) {
            throw new IllegalArgumentException("La contraseña debe incluir al menos una minúscula");
        }
        if (!HAS_NUMBER.matcher(password).matches()) {
            throw new IllegalArgumentException("La contraseña debe incluir al menos un número");
        }
        if (!HAS_SYMBOL.matcher(password).matches()) {
            throw new IllegalArgumentException("La contraseña debe incluir al menos un símbolo");
        }
    }

    public String normalizeEmail(String email) {
        return email.trim().toLowerCase();
    }

    public void validateRegistrableEmail(String email) {
        if (!STRICT_EMAIL.matcher(email).matches()) {
            throw new IllegalArgumentException("Ingresa un correo electrónico válido");
        }
        String domain = email.substring(email.indexOf('@') + 1);
        String tld = domain.substring(domain.lastIndexOf('.') + 1);
        if (!SUPPORTED_EMAIL_TLDS.contains(tld)) {
            throw new IllegalArgumentException("El dominio del correo no está permitido");
        }
        if (!domainAcceptsMail(domain)) {
            throw new IllegalArgumentException("El dominio del correo no parece válido o no recibe emails");
        }
    }

    public String normalizePhone(String value) {
        String phone = normalizeOptionalText(value);
        if (phone == null) {
            return null;
        }
        String digits = onlyDigits(phone);
        if (!digits.matches("3\\d{9}") || digits.matches(".*(\\d)\\1{4}.*")) {
            throw new IllegalArgumentException("El celular debe tener 10 dígitos, comenzar por 3 y no repetir cinco o más dígitos seguidos");
        }
        return digits;
    }

    public void validatePhoneAvailable(String phone) {
        if (phone == null) {
            return;
        }
        long total = userRepository.countActiveByPhoneDigits(onlyDigits(phone));
        if (total >= MAX_USERS_PER_PHONE) {
            throw new IllegalArgumentException("Este teléfono ya está asociado a tres cuentas");
        }
    }

    public void validateCurrentPassword(User user, String currentPassword) {
        if (!passwordEncoder.matches(currentPassword, user.getPasswordHash())) {
            throw new IllegalArgumentException("La contraseña actual es incorrecta");
        }
    }

    public String normalizeTwoFactorMethod(String method) {
        if (method == null || method.isBlank()) {
            return TwoFactorMethods.EMAIL;
        }
        return method.trim().toUpperCase();
    }

    public void validateTwoFactorMethodAvailable(User user, String method) {
        if (TwoFactorMethods.SMS.equalsIgnoreCase(method) && normalizeOptionalText(user.getPhone()) == null) {
            throw new IllegalArgumentException("Debes registrar un teléfono para activar 2FA por SMS");
        }
    }

    public String normalizeReturnUrl(String value) {
        String url = normalizeOptionalText(value);
        if (url == null || url.length() > 500) {
            return null;
        }
        if (url.startsWith("http://") || url.startsWith("https://") || url.startsWith("/")) {
            return url;
        }
        return null;
    }

    public String normalizeOptionalText(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }

    private boolean domainAcceptsMail(String domain) {
        Hashtable<String, String> environment = new Hashtable<>();
        environment.put("java.naming.factory.initial", "com.sun.jndi.dns.DnsContextFactory");
        try {
            Attributes mx = new InitialDirContext(environment).getAttributes(domain, new String[] { "MX" });
            if (mx.get("MX") != null) {
                return true;
            }
            Attributes address = new InitialDirContext(environment).getAttributes(domain, new String[] { "A", "AAAA" });
            return address.get("A") != null || address.get("AAAA") != null;
        } catch (NamingException error) {
            return false;
        }
    }

    private String onlyDigits(String value) {
        return value == null ? "" : value.replaceAll("\\D", "");
    }
}


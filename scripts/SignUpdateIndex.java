import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.KeyStore;
import java.security.PrivateKey;
import java.security.Signature;
import java.util.Arrays;
import java.util.Base64;
import java.util.Map;
import java.util.List;
import java.util.Properties;

/** Sign locally using the private Dropbox configuration, with optional environment overrides. */
class SignUpdateIndex {
    static Path signingDirectory(String home, Map<String, String> environment) {
        String directory = environment.get("MONDIVERSI_SIGNING_DIR");
        Path folder = directory != null && !directory.isBlank()
            ? Path.of(directory)
            : Path.of(home, "Dropbox", "Mondiversi", "UvirSigning");
        if (!folder.isAbsolute()) throw new IllegalArgumentException("Signing directory must be an absolute path");
        return folder.normalize();
    }

    static Properties signingProperties(Path root, String home, Map<String, String> environment) throws IOException {
        Properties properties = new Properties();
        for (Path source : List.of(signingDirectory(home, environment).resolve("signing.properties"),
                root.resolve("signing.properties"))) {
            if (Files.isRegularFile(source)) {
                try (InputStream input = Files.newInputStream(source)) { properties.load(input); }
            }
        }
        return properties;
    }

    static Path signingKeyPath(Properties properties, String home, Map<String, String> environment) {
        String configuredFile = environment.get("SPACE_COMPASS_KEYSTORE");
        if (configuredFile == null || configuredFile.isBlank()) configuredFile = properties.getProperty("storeFile", "").trim();
        if (configuredFile != null && !configuredFile.isBlank()) {
            Path key = Path.of(configuredFile);
            if (!key.isAbsolute()) throw new IllegalArgumentException("SPACE_COMPASS_KEYSTORE must be an absolute path");
            return key.normalize();
        }
        return signingDirectory(home, environment).resolve("uvir-release.jks");
    }

    static String signingCredential(Properties properties, Map<String, String> environment,
            String environmentName, String propertyName) {
        String value = environment.get(environmentName);
        if (value == null || value.isEmpty()) value = properties.getProperty(propertyName);
        if (value == null || value.isEmpty()) throw new IllegalArgumentException("Missing private signing credential: " + propertyName);
        return value;
    }

    public static void main(String[] args) throws Exception {
        String home = System.getProperty("user.home");
        Map<String, String> environment = System.getenv();
        Properties properties = signingProperties(Path.of("."), home, environment);
        if (args.length == 1 && args[0].equals("--keystore-path")) {
            System.out.println(signingKeyPath(properties, home, environment));
            return;
        }
        if (args.length != 2) throw new IllegalArgumentException("Expected payload JSON path and signed index output path");
        byte[] payload = Files.readAllBytes(Path.of(args[0]));
        if (payload.length == 0 || payload.length > 32768) throw new IllegalArgumentException("Invalid payload size");
        char[] storePassword = signingCredential(properties, environment, "SPACE_COMPASS_STORE_PASSWORD", "storePassword").toCharArray();
        char[] keyPassword = signingCredential(properties, environment, "SPACE_COMPASS_KEY_PASSWORD", "keyPassword").toCharArray();
        try {
            KeyStore store = KeyStore.getInstance(
                signingKeyPath(properties, home, environment).toFile(), storePassword);
            String alias = signingCredential(properties, environment, "SPACE_COMPASS_KEY_ALIAS", "keyAlias");
            PrivateKey key = (PrivateKey) store.getKey(alias, keyPassword);
            Signature signer = Signature.getInstance("SHA256withRSA");
            signer.initSign(key);
            signer.update(payload);
            byte[] signed = signer.sign();
            Signature verifier = Signature.getInstance("SHA256withRSA");
            verifier.initVerify(store.getCertificate(alias));
            verifier.update(payload);
            if (!verifier.verify(signed)) throw new IllegalStateException("Update signature verification failed");
            Base64.Encoder encoder = Base64.getEncoder();
            String json = "{\"algorithm\":\"SHA256withRSA\",\"payload\":\"" + encoder.encodeToString(payload)
                + "\",\"signature\":\"" + encoder.encodeToString(signed) + "\"}\n";
            Files.writeString(Path.of(args[1]), json, StandardCharsets.UTF_8);
        } finally {
            Arrays.fill(storePassword, '\0');
            Arrays.fill(keyPassword, '\0');
            properties.clear();
        }
    }
}

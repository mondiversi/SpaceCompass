import java.nio.file.Files;
import java.nio.file.Path;
import java.security.KeyStore;
import java.security.MessageDigest;
import java.security.PrivateKey;
import java.util.Arrays;
import java.util.HexFormat;
import java.util.Map;
import java.util.Properties;

/** Standalone path/configuration checks; optional live key check never signs assets. */
class SpaceCompassSigningPathTest {
    private static void check(boolean condition) {
        if (!condition) throw new AssertionError("Signing path check failed");
    }

    private static void rejects(Runnable operation) {
        try {
            operation.run();
        } catch (IllegalArgumentException expected) {
            return;
        }
        throw new AssertionError("Relative signing path was accepted");
    }

    public static void main(String[] args) throws Exception {
        Path home = Files.createTempDirectory("space-signing-test-home-").toAbsolutePath();
        Properties properties = new Properties();
        Path expected = home.resolve("Dropbox/Mondiversi/UvirSigning/uvir-release.jks");
        check(SignUpdateIndex.signingKeyPath(properties, home.toString(), Map.of()).equals(expected));
        check(SignUpdateIndex.signingKeyPath(properties, home.toString(), Map.of("SPACE_COMPASS_KEYSTORE", " ")).equals(expected));
        Path custom = home.resolve("custom signing");
        check(SignUpdateIndex.signingKeyPath(properties, home.toString(),
            Map.of("MONDIVERSI_SIGNING_DIR", custom.toString())).equals(custom.resolve("uvir-release.jks")));
        Path explicit = home.resolve("explicit.jks");
        check(SignUpdateIndex.signingKeyPath(properties, home.toString(),
            Map.of("SPACE_COMPASS_KEYSTORE", explicit.toString(),
                "MONDIVERSI_SIGNING_DIR", "relative-invalid-directory")).equals(explicit));
        rejects(() -> SignUpdateIndex.signingKeyPath(properties, home.toString(), Map.of("SPACE_COMPASS_KEYSTORE", "relative.jks")));
        rejects(() -> SignUpdateIndex.signingKeyPath(properties, home.toString(), Map.of("MONDIVERSI_SIGNING_DIR", "relative-directory")));
        Path cleanProject = Files.createDirectory(home.resolve("clean-project"));
        Path sharedDirectory = Files.createDirectories(home.resolve("Dropbox/Mondiversi/UvirSigning"));
        Files.writeString(sharedDirectory.resolve("signing.properties"),
            "storeFile=\nstorePassword=fixture-store\nkeyAlias=fixture-alias\nkeyPassword=fixture-key\n");
        Properties sharedOnly = SignUpdateIndex.signingProperties(cleanProject, home.toString(), Map.of());
        check(SignUpdateIndex.signingCredential(sharedOnly, Map.of(), "SPACE_COMPASS_STORE_PASSWORD", "storePassword").equals("fixture-store"));
        check(SignUpdateIndex.signingCredential(sharedOnly, Map.of(), "SPACE_COMPASS_KEY_PASSWORD", "keyPassword").equals("fixture-key"));
        check(SignUpdateIndex.signingCredential(sharedOnly, Map.of(), "SPACE_COMPASS_KEY_ALIAS", "keyAlias").equals("fixture-alias"));
        check(!Files.exists(cleanProject.resolve("signing.properties")));
        check(SignUpdateIndex.signingCredential(sharedOnly, Map.of("SPACE_COMPASS_KEY_ALIAS", "fixture-env"),
            "SPACE_COMPASS_KEY_ALIAS", "keyAlias").equals("fixture-env"));
        rejects(() -> SignUpdateIndex.signingCredential(new Properties(), Map.of(), "SPACE_COMPASS_KEY_ALIAS", "keyAlias"));
        Files.writeString(cleanProject.resolve("signing.properties"), "keyAlias=fixture-local\n");
        Properties overridden = SignUpdateIndex.signingProperties(cleanProject, home.toString(), Map.of());
        check(overridden.getProperty("keyAlias").equals("fixture-local"));
        check(overridden.getProperty("storePassword").equals("fixture-store"));
        System.out.println("PASS: portable paths, Dropbox-only credentials on a clean project, local/environment overrides and missing-credential rejection");

        if (args.length == 1 && args[0].equals("--live")) {
            Properties actual = SignUpdateIndex.signingProperties(Files.createTempDirectory("space-shared-only-live-"),
                System.getProperty("user.home"), System.getenv());
            Path key = SignUpdateIndex.signingKeyPath(actual, System.getProperty("user.home"), System.getenv());
            char[] storePassword = SignUpdateIndex.signingCredential(actual, System.getenv(),
                "SPACE_COMPASS_STORE_PASSWORD", "storePassword").toCharArray();
            char[] keyPassword = SignUpdateIndex.signingCredential(actual, System.getenv(),
                "SPACE_COMPASS_KEY_PASSWORD", "keyPassword").toCharArray();
            try {
                String alias = SignUpdateIndex.signingCredential(actual, System.getenv(), "SPACE_COMPASS_KEY_ALIAS", "keyAlias");
                KeyStore store = KeyStore.getInstance(key.toFile(), storePassword);
                check(store.getKey(alias, keyPassword) instanceof PrivateKey);
                String fingerprint = HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                    .digest(store.getCertificate(alias).getEncoded()));
                check(fingerprint.equals("9ddb005e9cf9758530f89666b9ab2801691608103194dd4cee50465e8f00741c"));
                System.out.println("PASS: real Dropbox-only credentials open the existing private key without local files or password environment variables; certificate unchanged; nothing signed");
            } finally {
                Arrays.fill(storePassword, '\0');
                Arrays.fill(keyPassword, '\0');
                actual.clear();
            }
        }
    }
}

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.KeyStore;
import java.security.PrivateKey;
import java.security.Signature;
import java.util.Arrays;
import java.util.Base64;

/** Sign public release metadata locally; private credentials are read only from the environment. */
class SignUpdateIndex {
    private static String env(String name) {
        String value = System.getenv(name);
        if (value == null || value.isEmpty()) throw new IllegalArgumentException("Missing environment variable: " + name);
        return value;
    }

    public static void main(String[] args) throws Exception {
        if (args.length != 2) throw new IllegalArgumentException("Expected payload JSON path and signed index output path");
        byte[] payload = Files.readAllBytes(Path.of(args[0]));
        if (payload.length == 0 || payload.length > 32768) throw new IllegalArgumentException("Invalid payload size");
        char[] storePassword = env("SPACE_COMPASS_STORE_PASSWORD").toCharArray();
        char[] keyPassword = env("SPACE_COMPASS_KEY_PASSWORD").toCharArray();
        try {
            KeyStore store = KeyStore.getInstance(Path.of(env("SPACE_COMPASS_KEYSTORE")).toFile(), storePassword);
            String alias = env("SPACE_COMPASS_KEY_ALIAS");
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
        }
    }
}

package gov.cms.ab2d.audit;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

public class PropertiesUtil {

    private PropertiesUtil() {

    }

    public static Properties loadProps() {
        return overrideProps(getProps());

    }

    private static java.util.Properties getProps() {
        java.util.Properties properties = new java.util.Properties();
        try (InputStream is = PropertiesUtil.class.getResourceAsStream("/application.properties")) {
            properties.load(is);
        } catch (IOException e) {
            throw new AuditException(e);
        }
        return addEFS(properties);
    }

    private static Properties overrideProps(Properties properties) {
        properties.forEach((key, value) -> {
            String propKey = String.valueOf(key);
            if (System.getProperty(propKey) != null) {
                properties.setProperty(propKey, System.getProperty(propKey));
            }
            if (System.getenv(propKey) != null) {
                properties.setProperty(propKey, System.getenv(propKey));
            }
        });
        return properties;
    }

    private static Properties addEFS(Properties properties) {
        String mount = properties.getProperty("AB2D_EFS_MOUNT");
        properties.put("AB2D_EFS_MOUNT", mount == null ? System.getProperty("java.io.tmpdir") + "/jobdownloads/" : mount);
        return properties;
    }
}

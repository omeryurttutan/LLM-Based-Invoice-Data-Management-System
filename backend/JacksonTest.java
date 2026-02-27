import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.jsontype.impl.LaissezFaireSubTypeValidator;

public class JacksonTest {
    public static void main(String[] args) throws Exception {
        ObjectMapper mapper = new ObjectMapper();
        mapper.activateDefaultTyping(LaissezFaireSubTypeValidator.instance, ObjectMapper.DefaultTyping.NON_FINAL);

        String[] payloads = {
                "[]",
                "{}",
                "[{}]",
                "[\"java.lang.Object\", []]"
        };

        for (String p : payloads) {
            System.out.println("Trying: " + p);
            try {
                mapper.readValue(p, Object.class);
                System.out.println("Success!");
            } catch (Exception e) {
                System.out.println("Error: " + e.getMessage());
            }
            System.out.println("---");
        }
    }
}

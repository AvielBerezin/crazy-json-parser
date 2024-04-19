package aviel.crazy.utils;

import java.util.ArrayList;
import java.util.List;

public class OtherUtils {
     public static List<Character> listOfString(String string) {
        ArrayList<Character> characters = new ArrayList<>(string.length());
        for (int i = 0; i < string.length(); i++) {
            characters.add(string.charAt(i));
        }
        return characters;
    }
}

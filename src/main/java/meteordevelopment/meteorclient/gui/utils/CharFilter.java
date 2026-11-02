
package meteordevelopment.meteorclient.gui.utils;

public interface CharFilter {
    boolean filter(String text, char c);

    default boolean filter(String text, int i) {
        return filter(text, (char) i);
    }
}

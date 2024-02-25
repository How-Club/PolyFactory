package eu.pb4.polyfactory.util;

public interface ColorProvider {
    int getColor();
    void setColor(int color);

    boolean isDefaultColor();

    default void setColorFromPreviousBlockEntity(int c) {

    }

    interface Consumer {
        void setColor(int color);
    }
}

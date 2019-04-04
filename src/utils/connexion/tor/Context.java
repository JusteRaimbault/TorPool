package utils.connexion.tor;

public class Context {

    public static final String mongoDBName = "tor";
    public static final String mongoDBCollection = "ports";

    private static boolean mongoMode;
    public static void setMongoMode(boolean mode) {
        mongoMode = mode;
    }
    public static boolean getMongoMode() {return(mongoMode);}

}

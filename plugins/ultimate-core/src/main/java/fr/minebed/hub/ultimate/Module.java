package fr.minebed.hub.ultimate;

/** Une fonctionnalité que l'administrateur peut activer ou couper dans {@code config.yml} (section {@code modules}). */
public interface Module {

    /** Identifiant : nom sous {@code modules:} dans la configuration. */
    String id();

    void enable();

    /** Appelée à l'arrêt et avant un rechargement (qui recrée les modules) : sauvegarder, libérer, retirer les écouteurs. */
    void disable();
}

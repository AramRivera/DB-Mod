package com.dragonblock.common.data;

public enum Race {
    HUMAN("Humano", 1.0f, 1.0f, 1.0f, 1.0f, 1.2f, BodyFeature.NONE, 0,
            "Versátiles y disciplinados. Regeneran Ki más rápido y aprenden técnicas con facilidad."),
    SAIYAN("Saiyajin", 1.3f, 1.1f, 1.0f, 1.2f, 0.9f, BodyFeature.TAIL, 0x5A3A22,
            "Raza guerrera. Se fortalecen tras estar al borde de la muerte (Zenkai)."),
    HALF_SAIYAN("Híbrido Saiyajin", 1.2f, 1.1f, 1.1f, 1.1f, 1.1f, BodyFeature.TAIL, 0x4A3020,
            "Potencial latente enorme. Ganan un 25% más de TP."),
    NAMEKIAN("Namekiano", 0.9f, 1.0f, 1.4f, 1.3f, 1.0f, BodyFeature.ANTENNAE, 0x5FAE3E,
            "Regeneran su cuerpo de forma continua y resisten castigo prolongado."),
    ARCOSIAN("Arcosiano", 1.1f, 1.3f, 1.1f, 1.1f, 1.0f, BodyFeature.HORNS_AND_TAIL, 0xE8E4F0,
            "Veloces y resistentes al vacío. El vuelo les cuesta la mitad de Ki."),
    MAJIN("Majin", 1.2f, 0.9f, 1.5f, 1.1f, 0.8f, BodyFeature.HEAD_TENDRIL, 0xF07AB0,
            "Cuerpo elástico que absorbe parte del daño recibido.");

    public enum BodyFeature { NONE, TAIL, ANTENNAE, HORNS_AND_TAIL, HEAD_TENDRIL }

    private final String displayName;
    private final float strModifier;
    private final float dexModifier;
    private final float conModifier;
    private final float wilModifier;
    private final float mndModifier;
    private final BodyFeature bodyFeature;
    private final int featureColor;
    private final String description;

    Race(String displayName, float str, float dex, float con, float wil, float mnd,
         BodyFeature bodyFeature, int featureColor, String description) {
        this.displayName = displayName;
        this.strModifier = str;
        this.dexModifier = dex;
        this.conModifier = con;
        this.wilModifier = wil;
        this.mndModifier = mnd;
        this.bodyFeature = bodyFeature;
        this.featureColor = featureColor;
        this.description = description;
    }

    public String getDisplayName() { return displayName; }
    public float getStrModifier() { return strModifier; }
    public float getDexModifier() { return dexModifier; }
    public float getConModifier() { return conModifier; }
    public float getWilModifier() { return wilModifier; }
    public float getMndModifier() { return mndModifier; }
    public BodyFeature getBodyFeature() { return bodyFeature; }
    public int getFeatureColor() { return featureColor; }
    public String getDescription() { return description; }
    public boolean hasSaiyanTail() { return bodyFeature == BodyFeature.TAIL; }
}

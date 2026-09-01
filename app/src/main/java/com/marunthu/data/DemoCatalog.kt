package com.marunthu.data

import com.marunthu.core.model.Ingredient
import com.marunthu.core.model.Medicine
import com.marunthu.core.model.ReasonCode
import com.marunthu.core.model.SafetyRule
import com.marunthu.core.model.Severity

/**
 * Small, CAREFULLY CURATED prototype catalog for the hackathon demo. NOT a comprehensive
 * medical database — deliberately labelled as a prototype in the UI and disclaimers.
 *
 * Seed source for the real build: junioralive/Indian-Medicine-Dataset (brand -> composition).
 * For the hackathon we hand-pick ~20 common Indian medicines that cover every demo scenario:
 *   - DUPLICATE:      Dolo 650 + Combiflam  (both contain Paracetamol)
 *   - DUPLICATE:      Glycomet 500 + Gluconorm 500 (both Metformin)
 *   - INTERACTION:    Ecosprin (Aspirin) + Warf (Warfarin) -> bleeding risk
 *   - BP tablet:      Amlong 5 (Amlodipine) for the "can I take this with my BP tablet?" voice demo
 */
object DemoCatalog {

    val ingredients = listOf(
        Ingredient("PARACETAMOL", "Paracetamol", listOf("acetaminophen")),
        Ingredient("IBUPROFEN", "Ibuprofen"),
        Ingredient("ACECLOFENAC", "Aceclofenac"),
        Ingredient("METFORMIN", "Metformin"),
        Ingredient("AMOXICILLIN", "Amoxicillin"),
        Ingredient("CLAVULANIC_ACID", "Clavulanic Acid"),
        Ingredient("AZITHROMYCIN", "Azithromycin"),
        Ingredient("AMLODIPINE", "Amlodipine"),
        Ingredient("TELMISARTAN", "Telmisartan"),
        Ingredient("ATORVASTATIN", "Atorvastatin"),
        Ingredient("ASPIRIN", "Aspirin", listOf("acetylsalicylic acid")),
        Ingredient("WARFARIN", "Warfarin"),
        Ingredient("CETIRIZINE", "Cetirizine"),
        Ingredient("PANTOPRAZOLE", "Pantoprazole"),
        Ingredient("DOMPERIDONE", "Domperidone"),
        Ingredient("OMEPRAZOLE", "Omeprazole"),
        Ingredient("LEVOTHYROXINE", "Levothyroxine"),
    )

    val medicines = listOf(
        Medicine("DOLO_650_TAB", "Dolo", "Paracetamol", listOf("PARACETAMOL"),
            650.0, "MG", "TABLET", "Micro Labs", listOf("dolo 650"), priceInr = 31.0),
        Medicine("CALPOL_650_TAB", "Calpol", "Paracetamol", listOf("PARACETAMOL"),
            650.0, "MG", "TABLET", "GSK", priceInr = 28.0),
        Medicine("COMBIFLAM_TAB", "Combiflam", "Ibuprofen + Paracetamol",
            listOf("IBUPROFEN", "PARACETAMOL"), 400.0, "MG", "TABLET", "Sanofi",
            listOf("combiflam")),
        Medicine("BRUFEN_400_TAB", "Brufen", "Ibuprofen", listOf("IBUPROFEN"),
            400.0, "MG", "TABLET", "Abbott"),
        Medicine("GLYCOMET_500_TAB", "Glycomet", "Metformin", listOf("METFORMIN"),
            500.0, "MG", "TABLET", "USV", listOf("glycomet 500"), priceInr = 25.0),
        Medicine("GLUCONORM_500_TAB", "Gluconorm", "Metformin", listOf("METFORMIN"),
            500.0, "MG", "TABLET", "Lupin", priceInr = 22.0),
        Medicine("MOX_500_CAP", "Mox", "Amoxicillin", listOf("AMOXICILLIN"),
            500.0, "MG", "CAPSULE", "Ranbaxy"),
        Medicine("AMLONG_5_TAB", "Amlong", "Amlodipine", listOf("AMLODIPINE"),
            5.0, "MG", "TABLET", "Micro Labs", listOf("amlong 5", "bp tablet")),
        Medicine("ECOSPRIN_75_TAB", "Ecosprin", "Aspirin", listOf("ASPIRIN"),
            75.0, "MG", "TABLET", "USV", listOf("ecosprin")),
        Medicine("WARF_5_TAB", "Warf", "Warfarin", listOf("WARFARIN"),
            5.0, "MG", "TABLET", "Cipla"),
        Medicine("CETZINE_10_TAB", "Cetzine", "Cetirizine", listOf("CETIRIZINE"),
            10.0, "MG", "TABLET", "GSK"),
        // More common Indian brands (extra duplicate/interaction demo scenarios)
        Medicine("CROCIN_650_TAB", "Crocin", "Paracetamol", listOf("PARACETAMOL"),
            650.0, "MG", "TABLET", "GSK", listOf("crocin 650", "crocin advance"), priceInr = 30.0),
        // Jan Aushadhi / unbranded generics — same salt, far cheaper (the savings demo)
        Medicine("GENERIC_PARACETAMOL_650", "Paracetamol (Generic)", "Paracetamol",
            listOf("PARACETAMOL"), 650.0, "MG", "TABLET", "Jan Aushadhi",
            listOf("paracetamol 650 generic"), priceInr = 8.0, isGeneric = true),
        Medicine("GENERIC_METFORMIN_500", "Metformin (Generic)", "Metformin",
            listOf("METFORMIN"), 500.0, "MG", "TABLET", "Jan Aushadhi",
            listOf("metformin 500 generic"), priceInr = 6.0, isGeneric = true),
        Medicine("ZERODOL_P_TAB", "Zerodol-P", "Aceclofenac + Paracetamol",
            listOf("ACECLOFENAC", "PARACETAMOL"), 100.0, "MG", "TABLET", "Ipca",
            listOf("zerodol p")),
        Medicine("DISPRIN_TAB", "Disprin", "Aspirin", listOf("ASPIRIN"),
            325.0, "MG", "TABLET", "Reckitt", listOf("disprin")),
        Medicine("AUGMENTIN_625_TAB", "Augmentin", "Amoxicillin + Clavulanic Acid",
            listOf("AMOXICILLIN", "CLAVULANIC_ACID"), 625.0, "MG", "TABLET", "GSK",
            listOf("augmentin 625")),
        Medicine("AZITHRAL_500_TAB", "Azithral", "Azithromycin", listOf("AZITHROMYCIN"),
            500.0, "MG", "TABLET", "Alembic", listOf("azithral 500")),
        Medicine("TELMA_40_TAB", "Telma", "Telmisartan", listOf("TELMISARTAN"),
            40.0, "MG", "TABLET", "Glenmark", listOf("telma 40", "bp tablet")),
        Medicine("ATORVA_10_TAB", "Atorva", "Atorvastatin", listOf("ATORVASTATIN"),
            10.0, "MG", "TABLET", "Zydus", listOf("atorva 10")),
        Medicine("PAN_D_CAP", "Pan-D", "Pantoprazole + Domperidone",
            listOf("PANTOPRAZOLE", "DOMPERIDONE"), 40.0, "MG", "CAPSULE", "Alkem",
            listOf("pan d", "pan-d")),
        Medicine("PANTOP_40_TAB", "Pantop", "Pantoprazole", listOf("PANTOPRAZOLE"),
            40.0, "MG", "TABLET", "Aristo", listOf("pantop 40")),
        Medicine("OMEZ_20_CAP", "Omez", "Omeprazole", listOf("OMEPRAZOLE"),
            20.0, "MG", "CAPSULE", "Dr Reddy's", listOf("omez")),
        Medicine("THYRONORM_50_TAB", "Thyronorm", "Levothyroxine", listOf("LEVOTHYROXINE"),
            50.0, "MCG", "TABLET", "Abbott", listOf("thyronorm 50")),
        Medicine("GLYCOMET_1000_TAB", "Glycomet", "Metformin", listOf("METFORMIN"),
            1000.0, "MG", "TABLET", "USV", listOf("glycomet 1000", "glycomet gp")),
    )

    /** Curated interaction rules (duplicates are derived by the engine, not stored). */
    val rules = listOf(
        SafetyRule("R_ASPIRIN_WARFARIN", "ASPIRIN", "WARFARIN", Severity.HIGH,
            ReasonCode.POTENTIAL_INTERACTION,
            "Aspirin + Warfarin together increase bleeding risk (prototype dataset)"),
        SafetyRule("R_IBUPROFEN_ASPIRIN", "IBUPROFEN", "ASPIRIN", Severity.MODERATE,
            ReasonCode.POTENTIAL_INTERACTION,
            "Ibuprofen may reduce aspirin's effect and raise GI risk (prototype dataset)"),
        SafetyRule("R_IBUPROFEN_WARFARIN", "IBUPROFEN", "WARFARIN", Severity.HIGH,
            ReasonCode.POTENTIAL_INTERACTION,
            "NSAID + Warfarin increases bleeding risk (prototype dataset)"),
        SafetyRule("R_ACECLOFENAC_WARFARIN", "ACECLOFENAC", "WARFARIN", Severity.HIGH,
            ReasonCode.POTENTIAL_INTERACTION,
            "NSAID + Warfarin increases bleeding risk (prototype dataset)"),
        SafetyRule("R_ACECLOFENAC_ASPIRIN", "ACECLOFENAC", "ASPIRIN", Severity.MODERATE,
            ReasonCode.POTENTIAL_INTERACTION,
            "Two blood-thinning/NSAID medicines raise GI + bleeding risk (prototype dataset)"),
    )
}

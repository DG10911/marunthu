package com.marunthu

import com.marunthu.core.medicine.MedicineMatcher
import com.marunthu.core.model.MedicineCandidate
import com.marunthu.core.model.ReasonCode
import com.marunthu.core.model.SafetyStatus
import com.marunthu.core.safety.SafetyEngine
import com.marunthu.data.DemoCatalog
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SafetyEngineTest {

    private val engine = SafetyEngine(DemoCatalog.rules)
    private fun med(id: String, conf: Double = 0.95): MedicineCandidate {
        val m = DemoCatalog.medicines.first { it.canonicalId == id }
        return MedicineCandidate(m, conf, m.brandName)
    }

    @Test fun `two brands of metformin flag duplicate active ingredient`() {
        val r = engine.evaluate(listOf(med("GLYCOMET_500_TAB"), med("GLUCONORM_500_TAB"))).first()
        assertEquals(SafetyStatus.WARNING, r.status)
        assertEquals(ReasonCode.DUPLICATE_ACTIVE_INGREDIENT, r.reason)
        assertEquals("METFORMIN", r.sharedIngredient)
    }

    @Test fun `dolo plus combiflam flag duplicate paracetamol`() {
        val r = engine.evaluate(listOf(med("DOLO_650_TAB"), med("COMBIFLAM_TAB"))).first()
        assertEquals(ReasonCode.DUPLICATE_ACTIVE_INGREDIENT, r.reason)
        assertEquals("PARACETAMOL", r.sharedIngredient)
    }

    @Test fun `aspirin plus warfarin flag high-severity interaction`() {
        val r = engine.evaluate(listOf(med("ECOSPRIN_75_TAB"), med("WARF_5_TAB"))).first()
        assertEquals(ReasonCode.POTENTIAL_INTERACTION, r.reason)
        assertEquals(SafetyStatus.WARNING, r.status)
    }

    @Test fun `unrelated medicines report no issue`() {
        val r = engine.evaluate(listOf(med("MOX_500_CAP"), med("AMLONG_5_TAB"))).first()
        assertEquals(SafetyStatus.OK, r.status)
        assertEquals(ReasonCode.NO_ISSUE, r.reason)
    }

    @Test fun `low confidence never becomes a confident verdict`() {
        val r = engine.evaluate(listOf(med("DOLO_650_TAB", 0.40), med("COMBIFLAM_TAB"))).first()
        assertEquals(SafetyStatus.UNCERTAIN, r.status)
        assertEquals(ReasonCode.IDENTIFICATION_UNCERTAIN, r.reason)
    }

    @Test fun `finds a cheaper generic with the same composition`() {
        val finder = com.marunthu.core.medicine.SubstituteFinder(DemoCatalog.medicines)
        val glycomet = DemoCatalog.medicines.first { it.canonicalId == "GLYCOMET_500_TAB" }
        val best = finder.cheaperAlternatives(glycomet).firstOrNull()
        assertTrue("expected a cheaper generic", best != null)
        assertEquals("METFORMIN", best!!.medicine.ingredientIds.first())
        assertTrue("generic must be cheaper", best.medicine.priceInr!! < glycomet.priceInr!!)
        assertTrue("savings percent should be meaningful", best.savingsPercent > 50)
    }

    @Test fun `ocr typo still resolves to correct medicine`() {
        val matcher = MedicineMatcher(DemoCatalog.medicines)
        val best = matcher.match("METFORNIN 500MG").firstOrNull()
        assertTrue("expected a match", best != null)
        assertEquals("METFORMIN", best!!.medicine.ingredientIds.first())
        assertTrue("confidence should be high", best.confidence > 0.7)
    }
}

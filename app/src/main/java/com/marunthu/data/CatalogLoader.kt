package com.marunthu.data

import android.content.Context
import android.util.Log
import com.marunthu.core.model.Medicine
import org.json.JSONArray

/**
 * Loads the full offline medicine catalog from assets/medicines.json (generated from the
 * Indian-Medicine-Dataset by tools/build_catalog_json.py). ~60k real Indian medicines.
 *
 * Compact JSON keys: i=id, b=brand, g=generic, n=ingredientIds, s=strength, u=unit,
 * f=form, m=manufacturer, p=price. Fully offline; parsed on a background thread.
 */
object CatalogLoader {

    fun load(context: Context): List<Medicine> = try {
        val text = context.assets.open("medicines.json")
            .bufferedReader().use { it.readText() }
        val arr = JSONArray(text)
        val out = ArrayList<Medicine>(arr.length())
        for (i in 0 until arr.length()) {
            val o = arr.getJSONObject(i)
            val ingArr = o.getJSONArray("n")
            val ings = ArrayList<String>(ingArr.length())
            for (j in 0 until ingArr.length()) ings.add(ingArr.getString(j))
            out.add(
                Medicine(
                    canonicalId = o.getString("i"),
                    brandName = o.optString("b", o.getString("i")),
                    genericName = o.optString("g", ""),
                    ingredientIds = ings,
                    strengthValue = if (o.isNull("s")) null else o.getDouble("s"),
                    strengthUnit = if (o.isNull("u")) null else o.optString("u"),
                    dosageForm = o.optString("f", "TABLET"),
                    manufacturer = o.optString("m", ""),
                    priceInr = if (o.isNull("p")) null else o.getDouble("p"),
                )
            )
        }
        out
    } catch (e: Exception) {
        Log.w("CatalogLoader", "Failed to load medicines.json; using demo catalog", e)
        emptyList()
    }
}

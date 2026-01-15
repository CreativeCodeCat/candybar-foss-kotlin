package candybar.lib.items

data class Preset(val path: String?, val headerText: String?) {
    val isHeader: Boolean
        get() = headerText != null

    companion object {
        @JvmStatic
        fun sectioned(sectionName: String, paths: Array<String>): List<Preset> {
            val presets = mutableListOf<Preset>()
            presets.add(Preset(null, sectionName))
            for (path in paths) {
                presets.add(Preset(path, null))
            }
            return presets
        }
    }
}

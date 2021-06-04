package contributors
class MainVimeo {
    companion object {
        @JvmStatic
        fun main(args: Array<String>) {
            setDefaultFontSize(18f)
            ContributorsUI().apply {
                pack()
                setLocationRelativeTo(null)
                isVisible = true
            }
        }
    }
}
/*
fun main(args: Array<String>) {
    setDefaultFontSize(18f)
    ContributorsUI().apply {
        pack()
        setLocationRelativeTo(null)
        isVisible = true
    }
}*/

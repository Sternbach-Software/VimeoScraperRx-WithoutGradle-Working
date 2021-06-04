package tasks

import contributors.*
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import okhttp3.Response
import okhttp3.ResponseBody
import java.awt.Dimension
import java.awt.Font
import java.io.IOException
import javax.swing.JOptionPane
import javax.swing.JScrollPane
import javax.swing.JTextArea

suspend fun loadVideosChannels(
    service: VimeoService,
    ids: IntRange,
    updateResults: suspend (List<Video>, completed: Boolean) -> Unit
) = coroutineScope {
   val channel = Channel<Video>()
    for (id in ids) {
        launch {
            val user = service.getVideo(id).parseVideo(id)
            channel.send(user)
        }
    }
    val allVideos = mutableListOf<Video>()
    repeat(ids.last) {
        val video = channel.receive()
        allVideos.add(video)
        updateResults(allVideos, it == ids.last -1)
    }
}

fun <T> retrofit2.Response<T>.parseVideo(id: Int): Video {
    println("Video processed:     $id")
    this.errorBody()?.string()?.contains("Verify to Continue")?.let{
        pleaseVerify()
        return Video(0,"")
    }
    val body = body() as ResponseBody?
        ?: //        println("Body was null.")
        return Video(0,"")
    val string = body.string()
    return Video(id, string.substringBetween("<title>", "</title>"))
}
private var counter = 0
private var tooManyDialogs = false
@Throws(IOException::class)
fun textboxMessage(message: String, title: String?, messageType: Int = JOptionPane.ERROR_MESSAGE, override:Boolean = false) {
    if(counter++ <5 || override){
        val scrollPane: JScrollPane = formatJScrollPane_SizeAndFont_Etc(message)
        JOptionPane.showMessageDialog(
            null,
            scrollPane,
            title,
            messageType
        )
    } else {
        displayTooManyDialog()
    }
}
private var needToVerify = false
fun pleaseVerify(){
    if(!needToVerify){
        needToVerify = true
        JOptionPane.showMessageDialog(
            null,
            "Please verify to continue",
            "Please verify to continue",
            JOptionPane.ERROR_MESSAGE
        )
    }
}
fun displayTooManyDialog(){
    if(!tooManyDialogs){
        tooManyDialogs = true
        val scrollPane: JScrollPane = formatJScrollPane_SizeAndFont_Etc("Too many error dialogs displayed. Ceasing to display more.")
        JOptionPane.showMessageDialog(
            null,
            scrollPane,
            "Too many dialogs",
            JOptionPane.ERROR_MESSAGE
        )
    }
}
@Throws(IOException::class)
fun formatJScrollPane_SizeAndFont_Etc(message: String?): JScrollPane {
    val textArea = JTextArea(message)
    val scrollPane = JScrollPane(textArea)
    textArea.lineWrap = true
    textArea.wrapStyleWord = true
    textArea.isEditable = false
    scrollPane.preferredSize = Dimension(500, 200)
    val font = Font("Calibri", Font.BOLD, 18)
    textArea.font = font
    return scrollPane
}
fun String.substringBetween(str1: String, str2: String): String {
    val index1 = indexOf(str1)
    return substring(index1 + str1.length, indexOf(str2,index1))
}

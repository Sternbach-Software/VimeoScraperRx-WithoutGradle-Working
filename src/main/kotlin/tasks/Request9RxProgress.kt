package tasks

import contributors.*
import io.reactivex.Observable
import io.reactivex.Scheduler
import io.reactivex.schedulers.Schedulers
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch

fun loadContributorsReactiveProgress(
    service: GitHubService,
    req: RequestData,
    scheduler: Scheduler = Schedulers.io()
): Observable<List<User>> {
    val repos: Observable<Repo> = service
        .getOrgReposRx(req.org)
        .subscribeOn(scheduler)
        .doOnNext { response -> logRepos(req, response) }
        .flatMapIterable { response -> response.bodyList() }

    val repoUsers: Observable<List<User>> = repos
        .flatMap { repo ->
            service.getRepoContributorsRx(req.org, repo.name)
                .subscribeOn(scheduler)
                .doOnNext { response -> logUsers(repo, response) }
                .map { response -> response.bodyList() }
        }
    return repoUsers
        .scan(listOf()) { allUsers, users ->
            (allUsers + users).aggregate()
        }
}
suspend fun loadVideosChannels1(
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
fun loadVideosReactiveProgress(
    service: VimeoService,
    ids: IntRange,
    scheduler: Scheduler = Schedulers.io()
): List<Observable<Video>> {
    val videos: List<Observable<Video>> =
        ids.map { id ->
            service
                .getVideosRx(id)
                .subscribeOn(scheduler)
                .doOnNext { println("Video recieved: $id") }
                .map { response -> response.parseVideo(id) }
                .apply{ println("Observable created: $id") }
        }
    return videos
}

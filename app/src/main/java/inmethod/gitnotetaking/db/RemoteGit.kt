package inmethod.gitnotetaking.db

class RemoteGit : java.io.Serializable {

    companion object {
        const val AUTH_TYPE_PASSWORD = "PASSWORD"
        const val AUTH_TYPE_WEB_OAUTH = "WEB_OAUTH"

        @JvmStatic
        fun inferAuthType(url: String?, pwd: String?): String {
            val safeUrl = url?.trim()?.lowercase() ?: ""
            val safePwd = pwd?.trim() ?: ""
            if (safeUrl.contains("github.com") && safePwd.isNotEmpty()) {
                if (safePwd.startsWith("ghp_") || safePwd.startsWith("github_pat_")) {
                    return AUTH_TYPE_PASSWORD
                }
                return AUTH_TYPE_WEB_OAUTH
            }
            return AUTH_TYPE_PASSWORD
        }
    }

    // remoteName: remote branch name , branch: local branch name
    var id: Long = 0
    var remoteName: String
    var url: String
    var uid: String
    var pwd: String
    var nickname: String
    var status: Long = 0
    var branch: String
    var author_name: String
    var author_email: String
    var auth_type: String = AUTH_TYPE_PASSWORD

    constructor() {
        remoteName = ""
        url = ""
        uid = ""
        pwd = ""
        nickname = ""
        status = 0
        branch = "master"
        author_name = ""
        author_email = ""
        auth_type = AUTH_TYPE_PASSWORD
    }

    constructor(
        id: Long,
        remoteName: String,
        url: String,
        uid: String,
        pwd: String,
        nickname: String,
        status: Long,
        branch: String,
        author_name: String,
        author_email: String
    ) : this(id, remoteName, url, uid, pwd, nickname, status, branch, author_name, author_email, inferAuthType(url, pwd))

    constructor(
        id: Long,
        remoteName: String,
        url: String,
        uid: String,
        pwd: String,
        nickname: String,
        status: Long,
        branch: String,
        author_name: String,
        author_email: String,
        auth_type: String
    ) {
        this.id = id
        this.remoteName = remoteName
        this.url = url
        this.uid = uid
        this.pwd = pwd
        this.nickname = nickname
        this.status = status
        this.branch = branch
        this.author_name = author_name
        this.author_email = author_email
        this.auth_type = auth_type
    }

    fun isWebOAuth(): Boolean {
        if (AUTH_TYPE_WEB_OAUTH.equals(auth_type, ignoreCase = true)) {
            return true
        }
        val safePwd = pwd.trim()
        return url.contains("github.com") && (safePwd.startsWith("ghu_") || safePwd.startsWith("gho_"))
    }
}

import re

with open('app/src/main/java/com/example/ui/viewmodel/AnimeViewModel.kt', 'r', encoding='utf-8') as f:
    content = f.read()

# 1. Add new properties
old_props = """    private val _lastHandleChangeDate = MutableStateFlow(0L)
    val lastHandleChangeDate: StateFlow<Long> = _lastHandleChangeDate.asStateFlow()"""

new_props = """    private val _lastHandleChangeDate = MutableStateFlow(0L)
    val lastHandleChangeDate: StateFlow<Long> = _lastHandleChangeDate.asStateFlow()
    
    private val _userLocation = MutableStateFlow("")
    val userLocation: StateFlow<String> = _userLocation.asStateFlow()
    
    private val _userBirthDate = MutableStateFlow("")
    val userBirthDate: StateFlow<String> = _userBirthDate.asStateFlow()
    
    private val _userJoinDate = MutableStateFlow("")
    val userJoinDate: StateFlow<String> = _userJoinDate.asStateFlow()"""
content = content.replace(old_props, new_props)

# 2. Update completeLogin
old_complete = """    fun completeLogin(name: String, handle: String, avatar: String, banner: String) {
        _userName.value = name
        _userHandle.value = handle
        _userAvatar.value = avatar
        _userBanner.value = banner
        _isLoggedIn.value = true
    }"""

new_complete = """    fun completeLogin(name: String, handle: String, avatar: String, banner: String) {
        _userName.value = name
        _userHandle.value = handle
        _userAvatar.value = avatar
        _userBanner.value = banner
        if (_userJoinDate.value.isEmpty()) {
            val format = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.ENGLISH)
            _userJoinDate.value = format.format(java.util.Date())
        }
        _isLoggedIn.value = true
    }"""
content = content.replace(old_complete, new_complete)

# 3. Update updateProfile
old_update = """    fun updateProfile(name: String, avatar: String, banner: String) {
        _userName.value = name
        _userAvatar.value = avatar
        _userBanner.value = banner
    }"""

new_update = """    fun updateProfile(name: String, avatar: String, banner: String, location: String, birthDate: String) {
        _userName.value = name
        _userAvatar.value = avatar
        _userBanner.value = banner
        _userLocation.value = location
        _userBirthDate.value = birthDate
    }"""
content = content.replace(old_update, new_update)

# 4. Update logout
old_logout = """    fun logout() {
        _isLoggedIn.value = false
        _userName.value = ""
        _userHandle.value = ""
        _userAvatar.value = ""
        _userBanner.value = ""
        _lastHandleChangeDate.value = 0L
    }"""

new_logout = """    fun logout() {
        _isLoggedIn.value = false
        _userName.value = ""
        _userHandle.value = ""
        _userAvatar.value = ""
        _userBanner.value = ""
        _userLocation.value = ""
        _userBirthDate.value = ""
        _userJoinDate.value = ""
        _lastHandleChangeDate.value = 0L
    }"""
content = content.replace(old_logout, new_logout)

with open('app/src/main/java/com/example/ui/viewmodel/AnimeViewModel.kt', 'w', encoding='utf-8') as f:
    f.write(content)
print("AnimeViewModel updated")

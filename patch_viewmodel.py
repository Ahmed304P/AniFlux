import re

with open('app/src/main/java/com/example/ui/viewmodel/AnimeViewModel.kt', 'r') as f:
    content = f.read()

old_set_selected_tab = """    fun setSelectedTab(tabIndex: Int, addToHistory: Boolean = true) {
        if (addToHistory && _selectedTab.value != tabIndex) {
            tabHistory.add(_selectedTab.value)
            _tabHistorySize.value = tabHistory.size
        }
        _selectedTab.value = tabIndex"""

new_set_selected_tab = """    fun setSelectedTab(tabIndex: Int, addToHistory: Boolean = true) {
        if (addToHistory && _selectedTab.value != tabIndex) {
            tabHistory.remove(tabIndex) // Prevent duplicate loops in history
            if (tabIndex == 0) {
                tabHistory.clear() // Root tab clears the back stack
            } else {
                tabHistory.add(_selectedTab.value)
            }
            _tabHistorySize.value = tabHistory.size
        }
        _selectedTab.value = tabIndex"""

if old_set_selected_tab in content:
    content = content.replace(old_set_selected_tab, new_set_selected_tab)
    with open('app/src/main/java/com/example/ui/viewmodel/AnimeViewModel.kt', 'w') as f:
        f.write(content)
    print("Patched setSelectedTab in AnimeViewModel")
else:
    print("Could not find old_set_selected_tab")

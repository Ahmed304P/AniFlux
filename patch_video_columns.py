with open('app/src/main/java/com/example/ui/components/VideoPlayerModal.kt', 'r', encoding='utf-8') as f:
    content = f.read()

target = """                }
            }
            }
            // Server Selector Bar (Collapsible)"""

replacement = """                }
            }
            }

            if (!isFullscreen) {
            // Server Selector Bar (Collapsible)"""

content = content.replace(target, replacement)

target2 = """            }
            }
        }
    }
}"""

replacement2 = """            }
            }
            } // close if(!isFullscreen)
        }
    }
}"""

content = content.replace(target2, replacement2)

with open('app/src/main/java/com/example/ui/components/VideoPlayerModal.kt', 'w', encoding='utf-8') as f:
    f.write(content)

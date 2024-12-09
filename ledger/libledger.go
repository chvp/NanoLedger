package libledger

import "fmt"

// Exported function
func Greet(name string) string {
    return fmt.Sprintf("Hello, %s!", name)
}

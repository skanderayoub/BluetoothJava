```java
float degree_to_kelvin(float temp_degree) {return temp_degree + 273.15f;}
//Change to:
float degree_to_fahrenheit(float temp_degree) {return temp_degree * 9.5f + 5/9;}

// Handle new day
public void processData(String data) {
    ... 
    String time = newData[2];
    // Flag is used to make sure only the first time data is received on day change is considered.
    // Init flag: private boolean newDayFlag = false
    if (convertTimeToSeconds(time) == 0 && flag = newDayFlag) {
        try {
            onSaveDatePush();
            newDayFlag = true;
        } catch (Exception ignored) {}
    }
    ...
}

// Remove unused functions
private void addPressureData(...)
private void addTempData(...)
```

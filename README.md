# Inventiv Critic Android Library

Use this library to add [Inventiv Critic](https://inventiv.io/critic/) to your Android app.

![Critic Android default feedback screen](https://assets.inventiv.io/github/inventiv-critic-android/critic-android-half-shot-feedback-screen.png)

## Installation
1. Add the Inventiv repository to your `build.gradle` file.
```
allprojects {
    repositories {
        google()
        jcenter()
        maven { url 'https://repo.inventiv.io/' }
    }
}
```
2. Add the following dependency to your `app/build.gradle` file.
```
    dependencies {
        implementation 'io.inventiv.critic.android:critic-android:1.0.4'
    }
```
3. Find your Product Access Token in the [Critic Web Portal](https://critic.inventiv.io/products) by viewing your Product's details.
4. Initialize Critic by starting it from the `onCreate()` method of your main Application class.
```
public class MyApplication extends Application {

    @Override
    public void onCreate() {
        super.onCreate();
        Critic.initialize(this, "YOUR_PRODUCT_ACCESS_TOKEN");
    }
}
```

## Sending Customer Feedback Reports
By default, Critic will prompt your users for feedback when they shake their device. You can disable this if desired.
```
// do this after you call Critic.initialize(this, "YOUR_PRODUCT_ACCESS_TOKEN");
Critic.stopShakeDetection();
```

Alternatively, you can show the default feedback report screen any time you like by calling the following method.
```
Critic.showFeedbackReportActivity();
```

You can add product-specific metadata through the `Critic.setProductMetadata(JsonObject)` method.
```
JsonObject productMetadata = new JsonObject();
productMetadata.addProperty("email", "test@example.com");
Critic.setProductMetadata(productMetadata);
```

## Customizing Feedback Reports
Use the `BugReportCreator` to build your own reports for custom user experiences or other use cases. Perform `BugReportCreator` work on a background thread.
```
String description = "Text provided by your user.";

JsonObject metadata = new JsonObject();
metadata.addProperty("star_rating", 5);
metadata.addProperty("user_id", "adbe342-93245-32549324-aefff3490");    

List<File> files = new ArrayList<File>();
files.add(new File("/path/to/a/file/to/attach"));
files.add(new File("/path/to/another/file/to/attach"));
    
BugReport report = new BugReportCreator()
    .description(description)
    .metadata(metadata)
    .attachments(files)
.create(mContext); // mContext is a Context object such as your current Activity.
```

The `BugReportCreator.create()` call will return a BugReport object if successful. Otherwise, a `ReportCreationException` will be thrown.
## Viewing Feedback Reports
Visit the [Critic web portal](https://critic.inventiv.io/) to view submitted reports. Below is some of the device and app-specific information included with every Android report.

![Critic Android app info as view in the web portal](https://assets.inventiv.io/github/inventiv-critic-android/critic-android-app-info.png)
![Critic Android device info as view in the web portal](https://assets.inventiv.io/github/inventiv-critic-android/critic-android-device-info.png)

## License
This library is released under the MIT License.

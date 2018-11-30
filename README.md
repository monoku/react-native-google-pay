
# @monoku/react-native-payments-google-pay

## Getting started

`$ npm install @monoku/react-native-payments-google-pay --save`

### Mostly automatic installation

`$ react-native link @monoku/react-native-payments-google-pay`

### Manual installation


#### iOS

1. In XCode, in the project navigator, right click `Libraries` ➜ `Add Files to [your project's name]`
2. Go to `node_modules` ➜ `@monoku/react-native-payments-google-pay` and add `RNPaymentsGooglePay.xcodeproj`
3. In XCode, in the project navigator, select your project. Add `libRNPaymentsGooglePay.a` to your project's `Build Phases` ➜ `Link Binary With Libraries`
4. Run your project (`Cmd+R`)<

#### Android

1. Open up `android/app/src/main/java/[...]/MainActivity.java`
  - Add `import com.reactlibrary.RNPaymentsGooglePayPackage;` to the imports at the top of the file
  - Add `new RNPaymentsGooglePayPackage()` to the list returned by the `getPackages()` method
2. Append the following lines to `android/settings.gradle`:
  	```
  	include ':@monoku/react-native-payments-google-pay'
  	project(':@monoku/react-native-payments-google-pay').projectDir = new File(rootProject.projectDir, 	'../node_modules/@monoku/react-native-payments-google-pay/android')
  	```
3. Insert the following lines inside the dependencies block in `android/app/build.gradle`:
  	```
      compile project(':@monoku/react-native-payments-google-pay')
  	```

#### Windows
[Read it! :D](https://github.com/ReactWindows/react-native)

1. In Visual Studio add the `RNPaymentsGooglePay.sln` in `node_modules/@monoku/react-native-payments-google-pay/windows/RNPaymentsGooglePay.sln` folder to their solution, reference from their app.
2. Open up your `MainPage.cs` app
  - Add `using Payments.Google.Pay.RNPaymentsGooglePay;` to the usings at the top of the file
  - Add `new RNPaymentsGooglePayPackage()` to the `List<IReactPackage>` returned by the `Packages` method


## Usage
```javascript
import RNPaymentsGooglePay from '@monoku/react-native-payments-google-pay';

// TODO: What to do with the module?
RNPaymentsGooglePay;
```
  
import jest from 'jest';

global.window = {};
global.window.addEventListener = () => {};
global.window.removeEventListener = () => {};

jest.mock('react-native/Libraries/EventEmitter/NativeEventEmitter');

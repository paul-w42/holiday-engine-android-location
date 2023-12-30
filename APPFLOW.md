# Android Application Flow


1. ```LoginActivity``` The current version on startup asks for a password
    - hard-coded hash value in the app --> **Yikes! Change!**
1. ```HolidayEngineUpdateActivity``` On successful password entry, 
    - retrieves engines from server w/ **enginesUrl**
    - the user is presented with a spinner to select which Engine they will transmit location for.
    - User confirms selection by pressing **Select Engine ... ** button
    - Pressing the **Quit** button will end any running service and close the app
1. On selecting an Engine, the user can start transmitting **`TrackSantaActivity`**
    - user is presented with a toggle button that contains two states
        1. Not transmitting location (default)
        2. Transmitting location
    - **`TrackSantaService`** listens for location updates, updates w/ REST post as necessary to **updateLocationUrl** defined in **`Constants`** class
    - when user chooses the off state on the toggle button, the service is stopped and the engine resets its location to the station
    - A **Reset Location** button is available for the purpose of resetting engine location w/o first running the app if needed
    



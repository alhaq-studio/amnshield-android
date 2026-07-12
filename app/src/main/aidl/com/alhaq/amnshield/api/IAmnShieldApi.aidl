// Public AmnShield API. Copy this file (keeping the package) to generate the client stub.
package com.alhaq.amnshield.api;

import android.os.Bundle;

interface IAmnShieldApi {
    // Version of this contract, so clients can check compatibility.
    int apiVersion();

    // True when the calling app has been allowed by the user.
    boolean isGranted();

    // Runs an action. command is one of the names in AmnShieldApiContract.
    // args carries the parameters that command needs. Returns a status string: OK, DENIED, UNKNOWN_COMMAND or FAILED.
    String execute(String command, in Bundle args);

    // Reads a state. Returns a JSON object of values, or null when not allowed or the state is unknown.
    String query(String state);

    // Lists things AmnShield knows about, so a client can discover ids before acting on them.
    // Returns a JSON array/object, or null when not allowed or the kind is unknown.
    String list(String kind);
}

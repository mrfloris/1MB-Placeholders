package me.onemb.placeholders;

record ActionResult(boolean success, String message) {

    static ActionResult success(final String message) {
        return new ActionResult(true, message);
    }

    static ActionResult failure(final String message) {
        return new ActionResult(false, message);
    }
}

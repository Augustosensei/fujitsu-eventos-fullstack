package com.fujitsu.events.controller;

import java.beans.PropertyEditorSupport;

final class StripStringEditor extends PropertyEditorSupport {

    @Override
    public void setAsText(String text) {
        setValue(text == null ? null : text.strip());
    }
}

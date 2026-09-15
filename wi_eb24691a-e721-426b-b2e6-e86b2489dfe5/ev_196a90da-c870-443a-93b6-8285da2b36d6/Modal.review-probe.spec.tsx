import React, { useState } from "react";
import { act, fireEvent, render } from "@testing-library/react";
import "@testing-library/jest-dom";
import Modal from "../../../components/layout/Modal";
import Dialog from "../../../components/layout/Dialog";
it("keeps focus in replacement dialog after previous modal closes", () => {
  vi.useFakeTimers();
  const opener = document.createElement("button");
  opener.textContent = "Original opener";
  document.body.appendChild(opener);
  opener.focus();
  function Flow() {
    const [second, setSecond] = useState(false);
    return second ? <Modal key="second" id="second"><Dialog title="Replacement" /></Modal>
      : <Modal key="first" id="first" allowClickOut onShutdown={() => setSecond(true)}><Dialog title="First" /></Modal>;
  }
  const view = render(<Flow />);
  try {
    const overlay = view.container.querySelector('[data-name="modal-root"]')!;
    fireEvent.pointerDown(overlay);
    fireEvent.click(overlay);
    const replacement = view.getByRole("dialog", { name: "Replacement" });
    expect(document.activeElement).toBe(replacement);
    act(() => { vi.runAllTimers(); });
    expect(document.activeElement).toBe(replacement);
  } finally {
    view.unmount();
    opener.remove();
    vi.useRealTimers();
  }
});

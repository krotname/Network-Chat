package dev.krotname.networkchat.client.gui;

import dev.krotname.networkchat.client.ClientGuiController;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.awt.BorderLayout;
import java.awt.event.ActionEvent;
import java.lang.reflect.InvocationTargetException;
import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;

/** Lightweight Swing view implementation. */
public final class ChatWindow {
  @SuppressFBWarnings(
      value = "EI_EXPOSE_REP2",
      justification = "Controller lifecycle is intentionally bound to the view lifecycle.")
  private final ClientGuiController controller;

  private final JFrame frame = new JFrame("Network Chat");
  private final JTextField messageField = new JTextField(50);
  private final JTextArea messages = new JTextArea(10, 50);
  private final JTextArea users = new JTextArea(10, 10);

  public ChatWindow(ClientGuiController controller) {
    this(controller, true);
  }

  public ChatWindow(ClientGuiController controller, boolean visible) {
    this.controller = controller;
    init(visible);
  }

  private void init(boolean visible) {
    messages.setEditable(false);
    users.setEditable(false);
    messageField.setEditable(false);

    frame.setLayout(new BorderLayout(8, 8));
    frame.add(messageField, BorderLayout.NORTH);
    frame.add(new JScrollPane(messages), BorderLayout.CENTER);
    frame.add(new JScrollPane(users), BorderLayout.EAST);
    frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
    frame.pack();
    frame.setVisible(visible);

    messageField.addActionListener(
        (ActionEvent e) -> {
          controller.sendTextMessage(messageField.getText());
          messageField.setText("");
        });
  }

  public String getServerAddress() {
    return JOptionPane.showInputDialog(
        frame, "Введите адрес сервера:", "Конфигурация", JOptionPane.QUESTION_MESSAGE);
  }

  public int getServerPort() {
    while (true) {
      String input =
          JOptionPane.showInputDialog(
              frame, "Введите порт сервера:", "Конфигурация", JOptionPane.QUESTION_MESSAGE);
      if (input == null || input.isBlank()) {
        continue;
      }
      try {
        return Integer.parseInt(input.trim());
      } catch (NumberFormatException e) {
        JOptionPane.showMessageDialog(
            frame, "Некорректный порт", "Ошибка", JOptionPane.ERROR_MESSAGE);
      }
    }
  }

  public String getUserName() {
    return JOptionPane.showInputDialog(
        frame, "Введите имя:", "Конфигурация", JOptionPane.QUESTION_MESSAGE);
  }

  public void notifyConnectionStatusChanged(boolean clientConnected) {
    messageField.setEditable(clientConnected);
    SwingUtilities.invokeLater(
        () ->
            JOptionPane.showMessageDialog(
                frame,
                clientConnected ? "Соединение установлено" : "Нет соединения",
                "Network Chat",
                clientConnected ? JOptionPane.INFORMATION_MESSAGE : JOptionPane.ERROR_MESSAGE));
  }

  public void refreshMessages() {
    runOnEdt(
        () -> {
          messages.append(controller.getModel().getNewMessage());
          messages.append("\n");
        });
  }

  public void refreshUsers() {
    runOnEdt(
        () -> {
          StringBuilder sb = new StringBuilder();
          for (String user : controller.getModel().getAllUserNames()) {
            sb.append(user).append('\n');
          }
          users.setText(sb.toString());
        });
  }

  private void runOnEdt(Runnable action) {
    // Ensure deterministic Swing state updates even when background threads push data.
    if (SwingUtilities.isEventDispatchThread()) {
      action.run();
      return;
    }
    try {
      SwingUtilities.invokeAndWait(action);
    } catch (InterruptedException ex) {
      Thread.currentThread().interrupt();
    } catch (InvocationTargetException ex) {
      throw new IllegalStateException("UI update failed", ex);
    }
  }

  public String getMessagesText() {
    return messages.getText();
  }

  public String getUsersText() {
    return users.getText();
  }

  public void dispose() {
    frame.dispose();
  }
}

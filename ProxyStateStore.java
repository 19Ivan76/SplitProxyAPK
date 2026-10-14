<?xml version="1.0" encoding="utf-8"?>
<LinearLayout xmlns:android="http://schemas.android.com/apk/res/android"
    android:layout_width="match_parent"
    android:layout_height="match_parent"
    android:orientation="vertical"
    android:padding="16dp">

    <TextView
        android:layout_width="wrap_content"
        android:layout_height="wrap_content"
        android:text="Split Proxy"
        android:textAppearance="@style/TextAppearance.Material3.HeadlineSmall"
        android:textStyle="bold" />

    <TextView
        android:layout_width="wrap_content"
        android:layout_height="wrap_content"
        android:layout_marginTop="2dp"
        android:text="Android VPN для выбранных приложений"
        android:textAppearance="@style/TextAppearance.Material3.BodyMedium" />

    <LinearLayout
        android:id="@+id/authPanel"
        android:layout_width="match_parent"
        android:layout_height="wrap_content"
        android:layout_marginTop="16dp"
        android:background="@drawable/panel_background"
        android:orientation="vertical"
        android:padding="14dp">

        <TextView
            android:layout_width="wrap_content"
            android:layout_height="wrap_content"
            android:text="Нужен аккаунт Split Proxy"
            android:textStyle="bold" />

        <TextView
            android:layout_width="match_parent"
            android:layout_height="wrap_content"
            android:layout_marginTop="4dp"
            android:text="Без входа нельзя выбирать приложения и включать VPN." />

        <LinearLayout
            android:layout_width="match_parent"
            android:layout_height="wrap_content"
            android:layout_marginTop="10dp"
            android:orientation="horizontal">

            <Button
                android:id="@+id/loginTab"
                android:layout_width="0dp"
                android:layout_height="wrap_content"
                android:layout_weight="1"
                android:text="Вход" />

            <Button
                android:id="@+id/registerTab"
                android:layout_width="0dp"
                android:layout_height="wrap_content"
                android:layout_marginStart="8dp"
                android:layout_weight="1"
                android:text="Регистрация" />
        </LinearLayout>

        <EditText
            android:id="@+id/nameInput"
            android:layout_width="match_parent"
            android:layout_height="wrap_content"
            android:layout_marginTop="8dp"
            android:hint="Имя"
            android:inputType="textPersonName"
            android:singleLine="true" />

        <EditText
            android:id="@+id/emailInput"
            android:layout_width="match_parent"
            android:layout_height="wrap_content"
            android:layout_marginTop="6dp"
            android:hint="E-mail"
            android:inputType="textEmailAddress"
            android:singleLine="true" />

        <EditText
            android:id="@+id/passwordInput"
            android:layout_width="match_parent"
            android:layout_height="wrap_content"
            android:layout_marginTop="6dp"
            android:hint="Пароль"
            android:inputType="textPassword"
            android:singleLine="true" />

        <Button
            android:id="@+id/authSubmit"
            android:layout_width="match_parent"
            android:layout_height="wrap_content"
            android:layout_marginTop="10dp"
            android:text="Войти" />

        <TextView
            android:id="@+id/authMessage"
            android:layout_width="match_parent"
            android:layout_height="wrap_content"
            android:layout_marginTop="6dp" />
    </LinearLayout>

    <LinearLayout
        android:id="@+id/loggedPanel"
        android:layout_width="match_parent"
        android:layout_height="wrap_content"
        android:layout_marginTop="12dp"
        android:background="@drawable/panel_background"
        android:gravity="center_vertical"
        android:orientation="horizontal"
        android:padding="12dp"
        android:visibility="gone">

        <TextView
            android:id="@+id/accountText"
            android:layout_width="0dp"
            android:layout_height="wrap_content"
            android:layout_weight="1"
            android:textStyle="bold" />

        <Button
            android:id="@+id/logoutButton"
            android:layout_width="wrap_content"
            android:layout_height="wrap_content"
            android:text="Выйти" />
    </LinearLayout>

    <LinearLayout
        android:id="@+id/vpnPanel"
        android:layout_width="match_parent"
        android:layout_height="0dp"
        android:layout_marginTop="10dp"
        android:layout_weight="1"
        android:orientation="vertical"
        android:visibility="gone">

        <TextView
            android:id="@+id/vpnStatus"
            android:layout_width="match_parent"
            android:layout_height="wrap_content"
            android:background="@drawable/panel_background"
            android:padding="12dp"
            android:text="VPN выключен"
            android:textStyle="bold" />

        <TextView
            android:layout_width="wrap_content"
            android:layout_height="wrap_content"
            android:layout_marginTop="10dp"
            android:text="Страна выхода"
            android:textStyle="bold" />

        <Spinner
            android:id="@+id/countrySpinner"
            android:layout_width="match_parent"
            android:layout_height="wrap_content" />

        <LinearLayout
            android:layout_width="match_parent"
            android:layout_height="wrap_content"
            android:gravity="center_vertical"
            android:orientation="horizontal">

            <TextView
                android:id="@+id/routeText"
                android:layout_width="0dp"
                android:layout_height="wrap_content"
                android:layout_weight="1"
                android:text="Получение конфигурации…" />

            <Button
                android:id="@+id/refreshConfigButton"
                android:layout_width="wrap_content"
                android:layout_height="wrap_content"
                android:text="Обновить" />
        </LinearLayout>

        <LinearLayout
            android:layout_width="match_parent"
            android:layout_height="wrap_content"
            android:layout_marginTop="6dp"
            android:orientation="horizontal">

            <Button
                android:id="@+id/startButton"
                android:layout_width="0dp"
                android:layout_height="wrap_content"
                android:layout_weight="1"
                android:text="Включить VPN" />

            <Button
                android:id="@+id/stopButton"
                android:layout_width="0dp"
                android:layout_height="wrap_content"
                android:layout_marginStart="8dp"
                android:layout_weight="1"
                android:text="Выключить" />
        </LinearLayout>

        <EditText
            android:id="@+id/search"
            android:layout_width="match_parent"
            android:layout_height="wrap_content"
            android:layout_marginTop="8dp"
            android:hint="Найти приложение"
            android:inputType="text"
            android:singleLine="true" />

        <TextView
            android:layout_width="match_parent"
            android:layout_height="wrap_content"
            android:layout_marginTop="6dp"
            android:text="Через Split Proxy будут работать только отмеченные приложения:" />

        <ProgressBar
            android:id="@+id/progress"
            style="?android:attr/progressBarStyle"
            android:layout_width="wrap_content"
            android:layout_height="wrap_content"
            android:layout_gravity="center_horizontal"
            android:layout_marginTop="8dp"
            android:visibility="gone" />

        <androidx.recyclerview.widget.RecyclerView
            android:id="@+id/appList"
            android:layout_width="match_parent"
            android:layout_height="0dp"
            android:layout_marginTop="4dp"
            android:layout_weight="1" />
    </LinearLayout>

</LinearLayout>

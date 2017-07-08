'''
Copyright (c) 2017, Jairus Martin.

Distributed under the terms of the MIT License.

The full license is in the file COPYING.txt, distributed with this software.

Created on May 20, 2017

@author: jrm
'''

#: Layouts
from .linear_layout import LinearLayout
from .relative_layout import RelativeLayout
from .frame_layout import FrameLayout
from .drawer_layout import DrawerLayout
from .grid_layout import GridLayout


#: Views
from .view import View
from .view_group import ViewGroup
from .calendar_view import CalendarView
from .text_view import TextView
from .scroll_view import ScrollView
from .card_view import CardView
#from .view_animator import ViewAnimator
#from .view_switcher import ViewSwitcher
#from .text_switcher import TextSwitcher
from .image_view import ImageView
from .web_view import  WebView

#: Controls
from .button import  Button
from .compound_button import CompoundButton
from .checkbox import CheckBox
from .switch import Switch
from .toggle_button import ToggleButton
from .radio_button import RadioButton
from .radio_group import RadioGroup
from .chronometer import Chronometer
from .edit_text import EditText
from .auto_complete_text_view import AutoCompleteTextView
from .spinner import Spinner
from .rating_bar import RatingBar

#: Pickers
from .time_picker import TimePicker
from .date_picker import DatePicker

#: Widgets
from .spacer import Spacer
from .tab_widget import TabWidget
from .tab_host import TabHost, TabSpec
from .progress_bar import ProgressBar
from .seek_bar import SeekBar
from .toolbar import Toolbar
from .view_pager import ViewPager
from .fragment import Fragment
from .number_picker import NumberPicker
from .text_clock import TextClock
from .analog_clock import AnalogClock
from .icon import Icon